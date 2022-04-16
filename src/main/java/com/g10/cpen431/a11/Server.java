package com.g10.cpen431.a11;

import com.g10.cpen431.a11.coordinator.CoordinatorService;
import com.g10.cpen431.a11.keystore.KeyMigrator;
import com.g10.cpen431.a11.keystore.PrimaryService;
import com.g10.cpen431.a11.keystore.ReplicaService;
import com.g10.cpen431.a11.membership.MembershipService;
import com.g10.cpen431.a11.rpc.RpcService;
import com.g10.util.LoggerConfiguration;
import com.g10.util.SystemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Server {
    private static Logger logger;

    public static void main(String[] args) throws IOException {
        initializeLogger(args[1]);

        NodeInfo.init(args[0], Integer.parseInt(args[1]));
        RpcService.init();
        MembershipService.init();
        CoordinatorService.init();
        KeyMigrator.init();
        PrimaryService.init();
        ReplicaService.init();

        SystemUtil.startSuspendWatchdog();

        logger.always().log("Server initialized. PID = {}", SystemUtil.getProcessId());
    }

    private static void initializeLogger(String label) {
        LoggerConfiguration.initialize(String.format("logs/server-%s.log", label));
        logger = LogManager.getLogger(Server.class);
        logger.always().log("Logger initialized.");
    }
}
