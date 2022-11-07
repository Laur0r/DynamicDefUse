// Adapted from https://github.com/scalameta/metals-languageclient/blob/main/src/interfaces/TreeViewProtocol.ts

"use strict";

import {
  Disposable,
  Command,
  RequestType,
  NotificationType,
  TextDocumentPositionParams,
} from "vscode-languageserver-protocol";

export interface TreeViews {
  disposables: Disposable[];
  reveal(params: TreeRevealResult): void;
}

export interface TreeViewNode {
  /** The ID of the view that this node is associated with. */
  viewId: string;
  /** The URI of this node, or undefined if root node of the view. */
  nodeUri?: string;
  /** The title to display for this node. */
  label: string;
  /** An optional command to trigger when the user clicks on this tree view node. */
  command?: Command;
  /** An optional SVG icon to display next to the label of this tree node. */
  icon?: string;
  /** An optional description of this tree node that is displayed when the user hovers over this node. */
  tooltip?: string;
  /**
   * Whether this tree node should be collapsed, expanded or if it has no children.
   *
   * - undefined: this node has no children.
   * - collapsed: this node has children and this node should be auto-expanded
   *   on the first load.
   * - collapsed: this node has children and the user should manually expand
   *   this node to see the children.
   */
  collapseState?: "collapsed" | "expanded";
}

export interface TreeViewChildrenParams {
  /** The ID of the view that is node is associated with. */
  viewId: string;
  /** The URI of the parent node. */
  nodeUri?: string;
}

export interface TreeViewChildrenResult {
  /** The child nodes of the requested parent node. */
  nodes: TreeViewNode[];
}

export namespace TreeViewChildren {
  export const type = new RequestType<
    TreeViewChildrenParams,
    TreeViewChildrenResult,
    void
  >("experimental/treeViewChildren");
}

export interface TreeViewDidChangeParams {
  nodes: TreeViewNode[];
}
export namespace TreeViewDidChange {
  export const type = new NotificationType<TreeViewDidChangeParams>(
    "experimental/treeViewDidChange"
  );
}

export interface TreeViewParentParams {
  viewId: string;
  nodeUri: string;
}

export interface TreeViewParentResult {
  uri?: string;
}

export namespace TreeViewParent {
  export const type = new RequestType<
    TreeViewParentParams,
    TreeViewParentResult,
    void
  >("experimental/treeViewParent");
}

export interface TreeViewVisibilityDidChangeParams {
  /** The ID of the view that this node is associated with. */
  viewId: string;
  /** True if the node is visible, false otherwise. */
  visible: boolean;
}

export namespace TreeViewVisibilityDidChange {
  export const type =
    new NotificationType<TreeViewVisibilityDidChangeParams>(
      "experimental/treeViewVisibilityDidChange"
    );
}

export interface TreeViewNodeCollapseDidChangeParams {
  /** The ID of the view that this node is associated with. */
  viewId: string;
  /** The URI of the node that was collapsed or expanded. */
  nodeUri: string;
  /** True if the node is collapsed, false if the node was expanded. */
  collapsed: boolean;
}

export namespace TreeViewNodeCollapseDidChange {
  export const type =
    new NotificationType<TreeViewNodeCollapseDidChangeParams>(
      "experimental/treeViewNodeCollapseDidChange"
    );
}

export interface TreeRevealResult {
  /** The ID of the view that this node is associated with. */
  viewId: string;
  /**
   * The list of URIs for the node to reveal and all of its ancestor parents.
   *
   * The node to reveal is at index 0, it's parent is at index 1 and so forth
   * up until the root node.
   */
  uriChain: string[];
}

export namespace TreeViewReveal {
  export const type = new RequestType<
    TextDocumentPositionParams,
    TreeRevealResult,
    void
  >("experimental/treeViewReveal");
}
