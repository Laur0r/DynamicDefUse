'use strict';

import * as path from 'path';
import * as os from 'os';
import { ExtensionContext, commands, ProgressLocation, window, workspace, StatusBarAlignment, TextEditor, StatusBarItem } from 'vscode';

import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions
} from 'vscode-languageclient/node';
import { TreeViews } from './DaciteTreeViewProtocol';
import { startTreeView } from './treeview';

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

  commands.registerCommand('dacite.analyze.proxy', async () => {
    console.log("Running command...");
    console.log((await commands.getCommands()).filter(it => it.includes('analyze')));
    const returnVal = await commands.executeCommand("dacite.analyze", window.activeTextEditor?.document.uri.toString());
    console.log(`Received return value "${returnVal}" from language server.`);
  });

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

function generate(command: string, ...additionalParameters: any[]): (...args: any[]) => any {
    return async (...args: any[]) => {
        console.log("Starting command");
        if ((await commands.getCommands()).filter(it => it === command).length === 0) {
            window.showWarningMessage(`The language server is not ready yet. Please wait a few seconds and try again.`);
            return;
        }

        await window.withProgress({
            location: ProgressLocation.Notification,
            title: 'Executing...',
            cancellable: false
        },
        async () => {
            console.log(`Sending command "${command}" to language server.`);
            const returnVal: string = await commands.executeCommand(command, window.activeTextEditor?.document.uri.toString(), additionalParameters);
            console.log(`Received return value "${returnVal}" from language server.`);
        });
    };
}
