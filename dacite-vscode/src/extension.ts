'use strict';

import * as path from 'path';
import * as os from 'os';
import { ExtensionContext, window, workspace, StatusBarAlignment, TextEditor, StatusBarItem, ThemeColor, Range, TextEditorDecorationType } from 'vscode';

import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TextDocumentIdentifier
} from 'vscode-languageclient/node';
import { TreeViews } from './DaciteTreeViewProtocol';
import { startTreeView } from './treeview';
import { InlayHintDecoration } from './InlayHintDecoration';

let languageClient: LanguageClient;
let treeViews: TreeViews | undefined;
let decorationTypes = new Map<string, TextEditorDecorationType>();

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
    middleware: {
      async provideInlayHints(document, viewPort, token, next) {
        const identifier: TextDocumentIdentifier = {
          uri: document.uri.toString()
        };

        const inlayHints = await next(document, viewPort, token);

        inlayHints?.forEach((inlayHint) => {          
          languageClient.sendRequest(InlayHintDecoration.type, {identifier: identifier, position: inlayHint.position})
            .then((decoration) => {
              const key = `${document.uri}.${inlayHint.position.line}.${inlayHint.position.character}`;

              if(decoration) {
                let decorationType = decorationTypes.get(key);
                if(!decorationType) {
                  decorationType = window.createTextEditorDecorationType({
                    before: {
                      contentText: inlayHint.label.toString(),
                      color: decoration.color,
                      backgroundColor: new ThemeColor("editorInlayHint.background"),
                      textDecoration: ';background-clip: content-box;padding-right:0.5rem;',
                    },
                  });

                  decorationTypes.set(key, decorationType);
                }

                window.activeTextEditor?.setDecorations(
                  decorationType,
                  [new Range(inlayHint.position, inlayHint.position)]
                );
              }
          });
        });
        
        // We use our own rendering and, thus, disable the execution of VS Code's standard rending
        // by not passing any inlay hints
        return undefined;
      },
    }
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

  treeViews = startTreeView(languageClient, languageClient.outputChannel, context, ['defUseChains']);
  context.subscriptions.concat(treeViews.disposables);

  window.onDidChangeActiveTextEditor((editor) =>{
		toggleItem(editor, statusBarItem);
	});
}

export async function deactivate() {
  if (languageClient) {
		await languageClient.stop();
	}
}

function toggleItem(editor: TextEditor | undefined, statusBarItem: StatusBarItem) {
	if(editor && editor.document && editor.document.languageId === 'java'){
		statusBarItem.show();
	} else{
		statusBarItem.hide();
	}
}
