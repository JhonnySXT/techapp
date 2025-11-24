#!/bin/sh

if [ -z "$husky_skip_init" ]; then
  if [ "$HUSKY" = 1 ]; then
    return
  fi
  export HUSKY=1
  . "$0" --single "$@"
  exit
fi






