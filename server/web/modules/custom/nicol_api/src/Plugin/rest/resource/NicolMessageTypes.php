<?php
// Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
// SPDX-License-Identifier: EUPL-1.2


namespace Drupal\nicol_api\Plugin\rest\resource;

use Drupal\Core\Session\AccountProxyInterface;
use Drupal\rest\Plugin\ResourceBase;
use Drupal\rest\ResourceResponse;
use Symfony\Component\DependencyInjection\ContainerInterface;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\HttpException;
use Psr\Log\LoggerInterface;

/**
 * Provides a resource to get view modes by entity and bundle.
 *
 * @RestResource(
 *   id = "nicol_messagetypes",
 *   label = @Translation("Nicol messagetypes rest resource"),
 *   uri_paths = {
 *     "canonical" = "/api/v1/messagetypes"
 *   }
 * )
 */
class NicolMessageTypes extends ResourceBase {

  /**
   * A current user instance.
   *
   * @var \Drupal\Core\Session\AccountProxyInterface
   */
  protected $currentUser;

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
   * Responds to GET requests.
   * @param $data
   *   An array containing received information.
   *
   * @param $request
   *
   * @return array
   *   Returns a list of Nicol MessageTypes.
   *
   * @throws \Symfony\Component\HttpKernel\Exception\HttpException
   *   Throws exception expected.
   */
  public function get($data, $request) {
    // Use current user after pass authentication to validate access.
    if (!$this->currentUser->hasPermission('access content')) {
      throw new AccessDeniedHttpException('Your device is not authorized.');
    }

    // Load user object.
    $user = user_load_by_name($this->currentUser->getAccountName());

    // Use current user after pass authentication to validate access.
    if ($user->isBlocked()) {
      throw new AccessDeniedHttpException('Your device is blocked.');
    }

    // Get container.
    $container = \Drupal::getContainer();

    // Get all parent terms: services.
    $items = $container->get('entity_type.manager')->getStorage('taxonomy_term')->loadTree('berichttypen', 0, 1, TRUE);

    // Retrieve all child terms: locations.
    if (!empty($items)) {

      // Get filter from querystring: ?filter[service]=1.
      // query string param
      $filter = $request->query->get('filter');

      foreach ($items as $parent) {
        $name = $parent->name->value;
        $linked_apps = $parent->field_linked_to_app->getValue();
        $apps = [];
        foreach ($linked_apps as $app) {
          $apps[] = $app['value'];
        }

        $tree[$parent->name->value] = [
          'id' => $parent->id(),
          'messagetypes' => []
        ];

        // Get all child terms: locations.
        $terms = $container->get('entity.manager')->getStorage('taxonomy_term')->loadTree('berichttypen', $parent->id(), NULL, TRUE);
        foreach ($terms as $child) {
          $tree[$parent->name->value]['messagetypes'][] = [
            'id' => $child->id(),
            'name' => $child->name->value
          ];
        }
      }

      $response = new ResourceResponse($tree);
      $response->addCacheableDependency($tree);
      return $response;

    }

    throw new HttpException(t('Error retrieving messagetypes.'));
  }

}
