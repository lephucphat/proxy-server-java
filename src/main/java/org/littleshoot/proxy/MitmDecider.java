package org.littleshoot.proxy;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Optional interface for a {@link MitmManager}: lets it decide, per CONNECT request, whether the connection
 * should be man-in-the-middled or tunneled untouched.
 */
public interface MitmDecider {
    boolean shouldMitm(HttpRequest connectRequest);
}
