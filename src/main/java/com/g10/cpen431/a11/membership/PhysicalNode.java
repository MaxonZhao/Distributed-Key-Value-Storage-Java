package com.g10.cpen431.a11.membership;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
@AllArgsConstructor
public class PhysicalNode {
    private final InetSocketAddress serverAddress;
    private final InetSocketAddress rpcAddress;
    private final InetSocketAddress tcpAddress;
    private final boolean isLocal;
}
