# EFLASH

## English

This source code has been made public in response to a request for disclosure under the Dutch Open Government Act (Woo).
Information that falls under the grounds for exemption in Article 5.1 of the Woo, has been marked in the source files
with the letters 'S' (security risk) and 'P' (privacy risk).

Reuse of the source code is allowed under the EUPL v1.2 license, with the exception of source code that is marked with a
different license.

Any vulnerabilities in the source code can be reported to the NCSC via:\
&emsp; https://www.ncsc.nl/contact/vulnerability-melden \
citing "EFLASH App".

## Nederlands

Deze broncode is openbaar gemaakt naar aanleiding van een verzoek tot openbaarmaking volgens de Wet Open Overheid (Woo).
Informatie die valt onder de uitzonderingsgronden van artikel 5.1 van de Woo, is gemarkeerd in de broncode met de letters
‘S’ (security-risico) en ‘P’ (privacy-risico).

Hergebruik van de broncode is toegestaan onder de EUPL v1.2 licentie, met uitzondering van broncode waarvoor een andere
licentie is aangegeven.

Eventuele kwetsbaarheden in de broncode kunnen worden gemeld bij het NCSC via:\
&emsp; https://www.ncsc.nl/contact/kwetsbaarheid-melden \
onder vermelding van "EFLASH App".


# Setup local environment

Dit project gebruikt een private composer registry in onze eigen Gitlab omgeving, lab.dtnr.nl

- Om deze packages te kunnen downloaden, moet je je eigen _personal access_token_ opzoeken in je profiel binnen Gitlab --> https://lab.dtnr.nl/-/profile/personal_access_tokens
- In de project root van dit project, het volgende (eenmalig) uitvoeren op de commandline:
  - `composer config gitlab-token.lab.dtnr.nl <personal_access_token>`
- het bestand `auth.json` wordt aangemaakt met je credentials
- Vervolgens kan je met `composer install` packages installeren of updaten met `composer update`

Onze eigen composer packages zijn te vinden onder:
[Dictu drupal modules](https://lab.dtnr.nl/dictu/drupal-modules)
Op dit moment hebben we deze eigen packages in gebruik:

- [apn_push](https://lab.dtnr.nl/dictu/drupal-modules/apn_push)
- [push_notifications](https://lab.dtnr.nl/dictu/drupal-modules/push_notifications)

---

---

## Zelf opzetten, configureren van een eigen composer registry

Achtergrond informatie over hoe zo'n registry aan te maken en packages te configurere en deployen:
[https://lab.dtnr.nl/help/user/packages/composer_repository/index]
