<?php

/**
 * @file
 * Contains \DrupalProject\composer\ScriptHandler.
 */

namespace DrupalProject\composer;

use Composer\Script\Event;
use Composer\Semver\Comparator;
use Symfony\Component\Filesystem\Filesystem;

class ScriptHandler {

  protected static function getDrupalRoot($project_root) {
    return $project_root . '/web';
  }

  public static function cleanupFiles(Event $event) {
    $fs = new Filesystem();
    $root = static::getDrupalRoot(getcwd());

    $files = [
      'core/.csslintrc',
      'core/.DS_Store',
      'core/.editorconfig',
      'core/.eslintignore',
      'core/.eslintrc.json',
      'core/.gitattributes',
      'core/install.php',
      'core/web.config',
      'core/INSTALL.txt',
      'core/INSTALL.mysql.txt',
      'core/INSTALL.pgsql.txt',
      'core/INSTALL.sqlite.txt',
      'core/MAINTAINERS.txt',
      'core/LICENSE.txt',
      'core/UPDATE.txt',
      'core/CHANGELOG.txt',
      'core/COPYRIGHT.txt',
      '.csslintrc',
      '.DS_Store',
      '.editorconfig',
      '.eslintignore',
      '.eslintrc.json',
      '.gitattributes',
      'install.php',
      'web.config',
      'INSTALL*.txt',
      'MAINTAINERS.txt',
      'LICENSE.txt',
      'UPDATE.txt',
      'CHANGELOG.txt',
      'COPYRIGHT.txt'
    ];

    // Required for unit testing
    foreach ($files as $file) {
      $fs->remove($root . '/'. $file);
    }
}

  public static function createRequiredFiles(Event $event) {
    $fs = new Filesystem();
    $root = static::getDrupalRoot(getcwd());

    $dirs = [
      'modules',
      'profiles',
      'themes',
    ];

    // Required for unit testing
    foreach ($dirs as $dir) {
      if (!$fs->exists($root . '/'. $dir)) {
        $fs->mkdir($root . '/'. $dir);
        $fs->touch($root . '/'. $dir . '/.gitkeep');
      }
    }

    $sites = array(
      'default',
    );

    foreach ($sites as $site) {
      // Prepare the settings file for installation
      if (!$fs->exists($root . '/sites/'.$site.'/settings.php') and $fs->exists($root . '/sites/'.$site.'/default.settings.php')) {
        $fs->copy($root . '/sites/'.$site.'/default.settings.php', $root . '/sites/'.$site.'/settings.php');
        $fs->chmod($root . '/sites/'.$site.'/settings.php', 0666);
        $event->getIO()->write("Create a sites/" . $site . "/settings.php file with chmod 0666");
      }

      // Prepare the services file for installation
      if (!$fs->exists($root . '/sites/'.$site.'/services.yml') and $fs->exists($root . '/sites/'.$site.'/default.services.yml')) {
        $fs->copy($root . '/sites/'.$site.'/default.services.yml', $root . '/sites/'.$site.'/services.yml');
        $fs->chmod($root . '/sites/'.$site.'/services.yml', 0666);
        $event->getIO()->write("Create a sites/" . $site . "/services.yml file with chmod 0666");
      }

      // Create the files directory with chmod 0777
      if (!$fs->exists($root . '/sites/'.$site.'/files')) {
        $oldmask = umask(0);
        $fs->mkdir($root . '/sites/'.$site.'/files', 0777);
        umask($oldmask);
        $event->getIO()->write("Create a sites/" . $site . "/files directory with chmod 0777");
      }
    }
  }

  /**
   * Checks if the installed version of Composer is compatible.
   *
   * Composer 1.0.0 and higher consider a `composer install` without having a
   * lock file present as equal to `composer update`. We do not ship with a lock
   * file to avoid merge conflicts downstream, meaning that if a project is
   * installed with an older version of Composer the scaffolding of Drupal will
   * not be triggered. We check this here instead of in drupal-scaffold to be
   * able to give immediate feedback to the end user, rather than failing the
   * installation after going through the lengthy process of compiling and
   * downloading the Composer dependencies.
   *
   * @see https://github.com/composer/composer/pull/5035
   */
  public static function checkComposerVersion(Event $event) {
    $composer = $event->getComposer();
    $io = $event->getIO();

    $version = $composer::VERSION;

    // The dev-channel of composer uses the git revision as version number,
    // try to the branch alias instead.
    if (preg_match('/^[0-9a-f]{40}$/i', $version)) {
      $version = $composer::BRANCH_ALIAS_VERSION;
    }

    // If Composer is installed through git we have no easy way to determine if
    // it is new enough, just display a warning.
    if ($version === '@package_version@' || $version === '@package_branch_alias_version@') {
      $io->writeError('<warning>You are running a development version of Composer. If you experience problems, please update Composer to the latest stable version.</warning>');
    }
    elseif (Comparator::lessThan($version, '1.0.0')) {
      $io->writeError('<error>Drupal-project requires Composer version 1.0.0 or higher. Please update your Composer before continuing</error>.');
      exit(1);
    }
  }

}
