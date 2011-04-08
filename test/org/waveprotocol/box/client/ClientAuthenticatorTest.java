/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.box.client;

import junit.framework.TestCase;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.mockito.Mockito;
import org.waveprotocol.box.consoleclient.ClientAuthenticator;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.AccountStoreHolder;
import org.waveprotocol.box.server.authentication.AuthTestUtil;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.authentication.SessionManagerImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.box.server.rpc.AuthenticationServlet;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.net.HttpCookie;

import javax.servlet.Servlet;

/**
 * Tests for ClientAuthenticator. This test creates a Jetty server and configures it with
 * a real AuthenticationServlet.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class ClientAuthenticatorTest extends TestCase {
  private static final String HOSTNAME = "localhost";

  private String endpoint;

  private Server server;

  @Override
  protected void setUp() throws Exception {
    AccountStore store = new MemoryStore();
    org.eclipse.jetty.server.SessionManager jettySessionManager =
        Mockito.mock(org.eclipse.jetty.server.SessionManager.class);
    AuthenticationServlet servlet =
        new AuthenticationServlet(AuthTestUtil.makeConfiguration(), new SessionManagerImpl(store,
            jettySessionManager), "example.com");

    store.putAccount(new HumanAccountDataImpl(
        ParticipantId.ofUnsafe("user@example.com"), new PasswordDigest("pwd".toCharArray())));
    store.putAccount(new HumanAccountDataImpl(
        ParticipantId.ofUnsafe("emptypwd@example.com"), new PasswordDigest("".toCharArray())));

    AccountStoreHolder.init(store, "example.com");

    startJettyServer(servlet);
  }

  @Override
  protected void tearDown() throws Exception {
    server.stop();
    AccountStoreHolder.resetForTesting();
  }

  public void testAuthenticate() throws Exception {
    ClientAuthenticator authenticator = new ClientAuthenticator(endpoint);

    HttpCookie token = authenticator.authenticate("user@example.com", "pwd".toCharArray());
    assertNotNull(token);
  }

  public void testAuthenticationFailReturnsNull() throws Exception {
    ClientAuthenticator authenticator = new ClientAuthenticator(endpoint);

    HttpCookie token = authenticator.authenticate("nonexistent@example.com", "pwd".toCharArray());
    assertNull(token);
  }

  public void testAuthenticationWorksWithEmptyPassword() throws Exception {
    ClientAuthenticator authenticator = new ClientAuthenticator(endpoint);

    HttpCookie token = authenticator.authenticate("emptypwd@example.com", "".toCharArray());
    assertNotNull(token);
  }

  // *** Helpers

  private void startJettyServer(Servlet authServlet) throws Exception {
    server = new Server();
    SelectChannelConnector connector = new SelectChannelConnector();
    connector.setHost(HOSTNAME);
    server.addConnector(connector);

    ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    handler.addServlet(new ServletHolder(authServlet), SessionManager.SIGN_IN_URL);
    server.setHandler(handler);

    server.start();
    endpoint = "http://" + HOSTNAME + ":" + connector.getLocalPort() + SessionManager.SIGN_IN_URL;
  }
}
