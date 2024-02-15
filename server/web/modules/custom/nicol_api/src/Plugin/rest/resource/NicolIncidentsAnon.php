<?php
// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


namespace Drupal\nicol_api\Plugin\rest\resource;

use Drupal\comment\Entity\Comment;
use Drupal\Core\Session\AccountProxyInterface;
use Drupal\rest\Plugin\ResourceBase;
use Drupal\rest\ResourceResponse;
use Symfony\Component\DependencyInjection\ContainerInterface;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;

use Psr\Log\LoggerInterface;

/**
 * Provides incidents for anonymous users, only containt incidents with
 *  access for anonymous users
 *
 * @RestResource(
 *   id = "nicol_incidents_anonymous",
 *   label = @Translation("Nicol incidents for anonymous users"),
 *   uri_paths = {
 *     "canonical" = "/api/v1/incidents_anon",
 *     "https://www.drupal.org/link-relations/create" = "/api/v1/incidents_anon"
 *   }
 * )
 */
class NicolIncidentsAnon extends ResourceBase {

  /**
   * A current user instance.
   *
   * @var \Drupal\Core\Session\AccountProxyInterface
   */
  protected $currentUser;
  // array with berichtenbox term_ids
  private $valid_term_ids;

  /**
   * Constructs a Drupal\rest\Plugin\ResourceBase object.
   *
   * @param array $configuration
   *   A configuration array containing information about the plugin instance.
   * @param string $plugin_id
   *   The plugin_id for the plugin instance.
   * @param mixed $plugin_definition
   *   The plugin implementation definition.
   * @param array $serializer_formats
   *   The available serialization formats.
   * @param \Psr\Log\LoggerInterface $logger
   *   A logger instance.
   * @param \Drupal\Core\Session\AccountProxyInterface $current_user
   *   A current user instance.
   */
  public function __construct(
    array $configuration,
    $plugin_id,
    $plugin_definition,
    array $serializer_formats,
    LoggerInterface $logger,
    AccountProxyInterface $current_user) {
    parent::__construct($configuration, $plugin_id, $plugin_definition, $serializer_formats, $logger);

    $this->currentUser = $current_user;
  }

  /**
   * {@inheritdoc}
   */
  public static function create(ContainerInterface $container, array $configuration, $plugin_id, $plugin_definition) {
    return new static(
      $configuration,
      $plugin_id,
      $plugin_definition,
      $container->getParameter('serializer.formats'),
      $container->get('logger.factory')->get('nicol_api'),
      $container->get('current_user')
    );
  }

  /**
   * Responds to POST requests.
   * @param $data
   *
   * @return \Drupal\rest\ResourceResponse Throws exception expected.
   *
   * @throws \Symfony\Component\HttpKernel\Exception\HttpException
   *   Throws exception expected.
   */
  public function post(array $data) {

    if ($data == NULL) {
      throw new BadRequestHttpException('No parameters received.');
    }

    // Use current user after pass authentication to validate access.
    if (!$this->currentUser->hasPermission('access content')) {
      throw new AccessDeniedHttpException('Your device is not authorized.');
    }

    // Load user object.
    $user = user_load_by_name($this->currentUser->getAccountName());

    // Check if user is blocked.
    if ($user->isBlocked()) {
      throw new AccessDeniedHttpException('Your device is blocked.');
    }

    $status        = $data['state'];
    $message_types = $data['messagetypes'];

    if (empty($message_types)) {
      return new ResourceResponse([]);
    }
    // filter only valid messagetypes (==> berichtenbox terms)
    // Get all parent terms: services.
    $bb_terms = taxonomy_term_load_multiple_by_name("Berichtenbox", 'berichttypen');
    // there can only be one
    $bb_term = reset($bb_terms);
    // stuur melding indien de berichtenbox term niet gevonden wordt.
    if (empty($bb_term)) {
      throw new HttpException(t('Berichtenbox type niet gevonden'));
    }
    // Get container.
    $container = \Drupal::getContainer();
    $terms = $container->get('entity.manager')->getStorage('taxonomy_term')->loadTree('berichttypen', $bb_term->id());
    // melding indien children van berichtenbox term niet worden gevonden
    if (empty($terms)) {
      throw new HttpException(t('Berichtenbox subtree niet gevonden'));
    }
    // Alleen de berichtenbox terms wordt op gezocht.
    $valid_tids = [$bb_term->id()];
    foreach ($terms as $child) {
      $valid_tids[] = $child->tid;
    }
    $this->valid_term_ids = $valid_tids;
    // filter op alleen geldige tids.
    $valid_message_types = array_intersect($valid_tids, $message_types);

    $result = $this->getIncidents($status, $valid_message_types);
    return new ResourceResponse($result);
  }

  /**
   * Private function getIncidents
   * @param array $status
   *   A configuration array containing information about the plugin instance.
   *
   * @param array $filter
   *   The term_ids which are  the location_ids or service_ids from vocabulary locations
   *
   * @return array
   *   Returns a list incidents filtered optional status and service/location data
   */
  private function getIncidents($status = ['open', 'closed'], $filter = []) {

    // Get services and locations from the requested filter array
    $result = [];

    $query = \Drupal::entityQuery('node')
      ->condition('type', 'incident')
      ->condition('status', 1)
      ->condition('field_status', $status, 'IN');
    $group = $query->orConditionGroup()
      ->condition('field_berichttype.entity.tid', $filter, 'IN')
    // is case an incident has no messagetype
      ->condition('field_berichttype', NULL, 'IS NULL');
    $nids = $query->condition($group)->sort('created', 'DESC')->execute();

    if (!empty($nids)) {

      // Load an array of node objects keyed by node ID.
      $nodes = \Drupal::entityTypeManager()->getStorage('node')->loadMultiple($nids);

      // build return_data array with only the values the app needs
      // nid, title, created, changed, body, status, locations, comments
      foreach ($nodes as $node) {

        $data = [
          'id'      => $node->id(),
          'title'   => $node->get('title')->value,
          'body'    => $node->get('body')->value,
          'created' => \Drupal::service('date.formatter')->format($node->get('created')->value, 'custom', 'Y-m-d\TH:i:sO'),
          'changed' => \Drupal::service('date.formatter')->format($node->get('changed')->value, 'custom', 'Y-m-d\TH:i:sO'),
          'status'  => $node->get('field_status')->value
        ];

        $message_types = $this->getMessageTypes($node->get('field_berichttype')->referencedEntities());
        $data['messagetypes'] = $message_types['messagetypes'];
        $data['messagestreams'] = $message_types['messagestreams'];
        $data['field_updates'] = $this->getComments($node->id());

        $result[] = $data;
      }
    }

    return $result;
  }

  /**
   * Private function getServiceLocations
   * @param array $data
   *   The term_ids from the locations vocabulary
   *
   * @return array
   *   Returns a array with separated services and locations.
   */
  private function getMessageTypes($data) {
    $stream = [];
    $types = [];

    foreach ($data as $value) {
      // check if value->id is in the valid term list..
      if (!in_array($value->id(), $this->valid_term_ids)) {
        continue;
      }
      // check if term_parent == 0 --> it is a service
      // if parent_term_id <> 0 --> it is a location.
      // Term has only one parent!!!!
      $parent = \Drupal::entityTypeManager()->getStorage('taxonomy_term')->loadParents($value->id());
      // only one parent is possible
      $parent = reset($parent);

      if (empty($parent)) {
        $stream[$value->id()] = ["tid" => $value->id(), "name" => $value->name->value];
      }
      else {
        $types[$value->id()] = ["tid" => $value->id(), "name" => $value->name->value];
        $stream[$parent->id()] = ["tid" => $parent->id(), "name" => $parent->name->value];
        // also get the parent stuf for this node_id
      }

    }

    $streams = array_values($stream);
    $types   = array_values($types);

    return [
      'messagetypes' => $types,
      'messagestreams' => $streams
    ];

  }

  /**
   * Private function getComments
   * @param int $node_id
   *
   * @return array
   *   Returns a list comments (updates) for given entity_id
   */
  private function getComments($node_id) {
    $comments = [];

    $cids = \Drupal::entityQuery('comment')
      ->condition('entity_id', $node_id)
      ->condition('entity_type', 'node')
      ->sort('cid', 'DESC')
      ->execute();

    foreach ($cids as $cid) {
      $comment = Comment::load($cid);
      $comments[] = [
        'cid' => $cid,
        //  'uid' => $comment->getOwnerId(),
        //  'subject' => $comment->get('subject')->value,
        'update' => $comment->get('comment_body')->value,
        'created' => \Drupal::service('date.formatter')->format($comment->get('created')->value, 'custom', 'Y-m-d\TH:i:sO')
      ];
    }

    return $comments;
  }

}
