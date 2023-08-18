"use strict";

import {
  Position,
  RequestType,
  TextDocumentIdentifier
} from "vscode-languageserver-protocol";

export interface InlayHintDecorationParams {
  identifier: TextDocumentIdentifier,
  position: Position
}

export interface InlayHintDecorationResult {
    color: string,
    fontStyle: string
}

export namespace InlayHintDecoration {
  export const type = new RequestType<
    InlayHintDecorationParams,
    InlayHintDecorationResult,
    void
  >("dacite/inlayHintDecoration");
}
