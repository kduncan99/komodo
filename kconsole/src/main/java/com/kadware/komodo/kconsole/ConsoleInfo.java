/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kconsole;

import com.kadware.komodo.commlib.SecureClient;
import javafx.stage.Stage;

/**
 * General information which must be shared among various classes
 */
public class ConsoleInfo {

    //  ConnectDialog window
    ConnectDialog _connectDialog = null;

    //  MainWindow window
    MainWindow _mainWindow = null;

    //  Primary Stage
    Stage _primaryStage = null;

    //  SecureClient object through which we communicate with the SystemProcessor
    SecureClient _secureClient = null;

    JumpKeyPane _jumpKeyPane = null;
}
