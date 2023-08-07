'use strict';

import * as path from 'path';
import * as os from 'os';
import * as vscode from 'vscode';
import { ExtensionContext, window, workspace, StatusBarAlignment, TextEditor, StatusBarItem, Position, Range, 
  ThemableDecorationAttachmentRenderOptions, TextEditorDecorationType} from 'vscode';

import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TextDocumentIdentifier,
  InlayHintParams
} from 'vscode-languageclient/node';
import { TreeViews } from './DaciteTreeViewProtocol';
import { startTreeView } from './treeview';
import { InlayHintDecorationParam, InlayHintDecoration } from './InlayHintDecoration';

let languageClient: LanguageClient;
let treeViews: TreeViews | undefined;

export async function activate(context: ExtensionContext) {
  const executable = os.platform() === 'win32' ? 'dacite-ls.bat' : 'dacite-ls';
  const serverModule = context.asAbsolutePath(path.join('dacite-ls', 'bin', executable));

  let serverOptions: ServerOptions = {
    run: {
      command: serverModule,
    },
    debug: {
      command: serverModule,
      args: ['-log'],
      options: {
        env: Object.assign(
          {
            JAVA_OPTS:'-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n,quiet=y'
          }, process.env)
        },
    },
  };

  let clientOptions: LanguageClientOptions = {
    documentSelector: ['java'],
     synchronize: {
      fileEvents: workspace.createFileSystemWatcher('**/*.java'),
    },
  };

  languageClient = new LanguageClient(
    'dacite',
    'Dacite Language Server',
    serverOptions,
    clientOptions
  );

  const statusBarItem = window.createStatusBarItem(StatusBarAlignment.Right, Number.MIN_VALUE);
	statusBarItem.name = 'Dacite Language Server';
	statusBarItem.text = 'Dacite LS $(sync~spin)';
	statusBarItem.tooltip = 'Language Server for Dacite is starting...';
	toggleItem(window.activeTextEditor, statusBarItem);

  console.log('Starting language server');
  try {
    await languageClient.start();

    statusBarItem.text = 'Dacite LS $(thumbsup)';
		statusBarItem.tooltip = 'Language Server for Dacite started';
		toggleItem(window.activeTextEditor, statusBarItem);
    languageClient.outputChannel.show();

  } catch(error) {
    statusBarItem.text = 'Dacite LS $(thumbsdown)';
		statusBarItem.tooltip = 'Language Server for Dacite failed to start';
		console.log(error);
  }

  
  treeViews = startTreeView(languageClient, languageClient.outputChannel, context, ['defUseChains', 'notCoveredDUC']);
  context.subscriptions.concat(treeViews.disposables);
  var decorationTypes:TextEditorDecorationType[] = [];

  vscode.workspace.onDidChangeTextDocument(function(TextDocumentChangeEvent) {
    const uriString = "file://"+TextDocumentChangeEvent.document.fileName;
    console.log(uriString);
    if(uriString !== undefined && !uriString.includes("extension-output-dacite-defuse")){
      const uri:TextDocumentIdentifier = {
        uri: uriString
      };
      const startPosition = new Position(0,0);
      const endPosition = new Position(TextDocumentChangeEvent.document.lineCount-1, 0);
      const range: Range = {
        start:startPosition, 
        end: endPosition
      }
      const param: InlayHintParams = {
        textDocument:uri,
        range: range
      }
      
      decorationTypes.forEach(function(item){
        item.dispose();
      })
      decorationTypes = [];
    languageClient.sendRequest("textDocument/inlayHint", param).then(data =>{
      if(Array.isArray(data)){
        console.log("in inlayhints");
        languageClient.protocol2CodeConverter.asInlayHints(data).then(data => {
          data.forEach(function(item){
            if(item instanceof vscode.InlayHint){
              const decorationParam: InlayHintDecorationParam = {
                identifier: uri,
                position: item.position
              }
              
              languageClient.sendRequest<InlayHintDecoration>("dacite/inlayHintDecoration", decorationParam).then(decoration =>{
                if((decoration as InlayHintDecoration) !== undefined){
                  const color = (decoration as InlayHintDecoration).color
                  const option: 
                  ThemableDecorationAttachmentRenderOptions = {
                    contentText: item.label.toString(),
                    color: color,
                    backgroundColor: new vscode.ThemeColor("editorInlayHint.background"),
                    textDecoration:';padding-right:0.5rem;'
                  }
                  
                  const decorationType = window.createTextEditorDecorationType({
                    before: option,
                  })
                  console.log(decorationTypes);
                  decorationTypes.push(decorationType);
                  const rangeDeco = new Range(item.position, item.position);
                
                  let rangeArray: Range[] = []
                  rangeArray.push(rangeDeco)
                  vscode.window.activeTextEditor?.setDecorations(decorationType, rangeArray);
                }
            });
          }
          })
        }
        )
      }
    })
    };
  })

  window.onDidChangeActiveTextEditor((editor) =>{
		toggleItem(editor, statusBarItem);
  })
    
}

export async function deactivate() {
  if (languageClient) {
		await languageClient.stop();
	}
}

function toggleItem(editor: TextEditor | undefined, statusBarItem: StatusBarItem) {
	if(editor && editor.document && editor.document.languageId === 'java'){
		statusBarItem.show();
	} else {
		statusBarItem.hide();
	}
}
