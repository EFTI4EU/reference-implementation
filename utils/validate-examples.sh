#!/usr/bin/env bash

SCHEMA_DIR=../schema/xsd
COMMON_XSD=$SCHEMA_DIR/consignment-common.xsd
IDENTIFIER_XSD=$SCHEMA_DIR/consignment-identifier.xsd
GATE_XSD=$SCHEMA_DIR/edelivery/gate.xsd
PLATFORM_XSD=$SCHEMA_DIR/edelivery/platform.xsd
EXAMPLE_DIR=../schema/xsd/examples

cd $(dirname $0)

some_invalid=0
for file in $(ls $EXAMPLE_DIR); do
  full_path="$EXAMPLE_DIR/$file"
  echo -n "Checking $full_path: "

  if [[ $(grep -c 'http://efti.eu/v1/edelivery' $full_path) != "0" ]]; then
    if [[ $(grep -c 'gate.xsd"' $full_path) != "0" ]]; then
      schema=$GATE_XSD
    else
      schema=$PLATFORM_XSD
    fi
  elif [[ $(grep -c 'xmlns="http://efti.eu/v1/consignment/identifier"' $full_path) != "0" ]]; then
    schema=$IDENTIFIER_XSD
  elif [[ $(grep -c 'xmlns="http://efti.eu/v1/consignment/common"' $full_path) != "0" ]]; then
    schema=$COMMON_XSD
  else
    schema=""
  fi

  if [[ "$schema" == "" ]]; then
    echo -n "schema not supported, skipping"
  else
    echo -n "validating with $schema - "

    xmlstarlet val -q -s $schema $full_path

    if [[ $? == 0 ]]; then
      echo -n "valid"
    else
      echo -n "invalid"
      some_invalid=1
    fi
  fi
  echo
done

if [[ "$some_invalid" == "0" ]]; then
  echo "All valid"
else
  echo "There were invalid documents"
  exit 1
fi