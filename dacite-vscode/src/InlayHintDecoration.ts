"use strict";

import {
  Position,
  TextDocumentIdentifier
} from "vscode-languageserver-protocol";

export interface InlayHintDecorationParam {
  identifier: TextDocumentIdentifier,
  position: Position
}

export interface InlayHintDecoration {
    color: number[],
    fontStyle: string
}