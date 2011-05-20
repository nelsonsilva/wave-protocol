/**
 * Copyright 2011 Google Inc.
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

package org.waveprotocol.box.server.robots;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.waveprotocol.box.server.robots.util.RobotsUtil.registerRobotUri;

import junit.framework.TestCase;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.util.RobotsUtil;
import org.waveprotocol.box.server.robots.util.RobotsUtil.RobotRegistrationException;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Unit tests for {@link RobotsUtil}.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class RobotsUtilTest extends TestCase {

  private final static String LOCATION = "https://example.com:9898/robot/";
  private final static ParticipantId ROBOT_ID = ParticipantId.ofUnsafe("nicerobot@example.com");

  private AccountStore accountStore;
  private TokenGenerator tokenGenerator;
  private AccountData accountData;

  @Override
  protected void setUp() throws Exception {
    accountStore = mock(AccountStore.class);
    tokenGenerator = mock(TokenGenerator.class);
    accountData = mock(AccountData.class);
  }

  public void testRegisterRobotUriFailsOnInvalidUri() throws RobotRegistrationException,
      PersistenceException {
    when(accountStore.getAccount(ROBOT_ID)).thenReturn(accountData);
    String invalidLocation = "ftp://some$$$&&&###.com";
    try {
      registerRobotUri(invalidLocation, ROBOT_ID, accountStore, tokenGenerator, true);
      fail("Location " + invalidLocation + " is invalid, exception is expected.");
    } catch (RobotRegistrationException e) {
      // Expected.
    }
  }

  public void testRegisterRobotUriSuceedsOnExistingAccountWhenForced()
      throws RobotRegistrationException, PersistenceException {
    when(accountStore.getAccount(ROBOT_ID)).thenReturn(accountData);
    String consumerToken = "sometoken";
    when(tokenGenerator.generateToken(anyInt())).thenReturn(consumerToken);

    AccountData resultAccountData =
        registerRobotUri(LOCATION, ROBOT_ID, accountStore, tokenGenerator, true);
    verify(accountStore).getAccount(ROBOT_ID);
    verify(accountStore).removeAccount(ROBOT_ID);
    verify(accountStore).putAccount(any(AccountData.class));
    verify(tokenGenerator).generateToken(anyInt());
    assertTrue(resultAccountData.isRobot());
    RobotAccountData robotAccountData = resultAccountData.asRobot();
    // Remove the last '/'.
    assertEquals(LOCATION.substring(0, LOCATION.length() - 1), robotAccountData.getUrl());
    assertEquals(ROBOT_ID, robotAccountData.getId());
    assertEquals(consumerToken, robotAccountData.getConsumerSecret());
  }

  public void testRegisterRobotUriFailsOnExistingAccountWhenNotForced()
      throws IllegalArgumentException, PersistenceException {
    when(accountStore.getAccount(ROBOT_ID)).thenReturn(accountData);
    try {
      registerRobotUri(LOCATION, ROBOT_ID, accountStore, tokenGenerator, false);
      fail();
    } catch (RobotRegistrationException e) {
      // Expected.
    }
    verify(accountStore).getAccount(ROBOT_ID);
  }
}
