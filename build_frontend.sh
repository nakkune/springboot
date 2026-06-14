#!/bin/bash
export NVM_DIR="$HOME/.nvm"
if [ -s "$NVM_DIR/nvm.sh" ]; then
    source "$NVM_DIR/nvm.sh"
fi
cd /home/knh11/spring/boot/frontend
npm run build
