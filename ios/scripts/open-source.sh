#!/bin/bash
# Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
# SPDX-License-Identifier: EUPL-1.2

# use Google's addlicense tool to add license header to (almost) all files
go install github.com/google/addlicense@latest
~/go/bin/addlicense -c "De Staat der Nederlanden, Dienst ICT Uitvoering" -l "EUPL-1.2" -s -v .