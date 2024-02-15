#!/bin/bash
# Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
# SPDX-License-Identifier: EUPL-1.2


# use jq to mask google service IDs in google-services.json.
# Build a jq expression for this.

jq_expression=""
for field in .project_info[] .client[].client_info.mobilesdk_app_id .client[].oauth_client[].client_id .client[].api_key[][]
do
    if [ "$jq_expression" != "" ]; then jq_expression="$jq_expression | "; fi;
    jq_expression=$jq_expression' '$field' |= (if type=="string" then (explode | map("S") | join("")) else . end)'
done

find . -name "google-services.json" -type f |
while read filename; do
    echo masking values in file ${filename}
    jq "$jq_expression" ${filename} > ${filename}.masked
    mv -f ${filename}.masked ${filename}
done

# use Google's addlicense tool to add license header to (almost) all files
go install github.com/google/addlicense@latest
~/go/bin/addlicense -c "De Staat der Nederlanden, Dienst ICT Uitvoering" -l "EUPL-1.2" -s -v .