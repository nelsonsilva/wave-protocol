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

package org.waveprotocol.box.server.robots.util;

import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;
import org.waveprotocol.wave.util.logging.Log;

import java.net.URI;

/**
 * Provides helper methods for the operation services.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class RobotsUtil {
  
@SuppressWarnings("serial")
public static class RobotRegistrationException extends Exception {
    
    public RobotRegistrationException (String message) {
      super(message);
    }

    public RobotRegistrationException(String message, Throwable t) {
      super(message, t);
    }
  }

  private static final Log LOG = Log.get(RobotsUtil.class);
  private static final IdURIEncoderDecoder URI_CODEC = new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionZeroFactoryImpl(
      URI_CODEC);
  private static final int TOKEN_LENGTH = 48;

  /**
   * Creates a new empty robot wavelet data.
   * 
   * @param participant the wavelet creator.
   * @param waveletName the wavelet name.
   */
  public static RobotWaveletData createEmptyRobotWavelet(ParticipantId participant,
      WaveletName waveletName) {
    HashedVersion hashedVersionZero = HASH_FACTORY.createVersionZero(waveletName);
    ObservableWaveletData emptyWavelet =
        WaveletDataUtil.createEmptyWavelet(waveletName, participant, hashedVersionZero,
            System.currentTimeMillis());
    RobotWaveletData newWavelet = new RobotWaveletData(emptyWavelet, hashedVersionZero);
    return newWavelet;
  }

  /**
   * Registers a robot.
   * 
   * @param robotName the robot name will be robotName@example.com.
   * @param location the URI that the robot listens on, for example:
   *        "http://example.com:80/robotName".
   * @param accountStore the account store.
   * @param tokenGenerator the robot consumer secret generator.
   * @param isForced if {@code true} then the existing robot account will be
   *        removed. if {@code false} and such robot account already exist and
   *        exception will be thrown.
   * @throws PersistenceException if the persistence layer reports an error.
   * @throws IllegalArgumentException if the robot id is already registered and
   *         <code>isForced</code> flag is {@code false} or if an invalid
   *         location is specified.
   */
  public static RobotAccountData registerRobotUri(String location, ParticipantId robotId,
      AccountStore accountStore, TokenGenerator tokenGenerator, boolean isForced)
      throws RobotRegistrationException, PersistenceException {
    if (accountStore.getAccount(robotId) != null) {
      if (isForced) {
        accountStore.removeAccount(robotId);
      } else {
        throw new RobotRegistrationException(robotId.getAddress()
            + " is already in use, please choose another one.");
      }
    }

    URI uri;
    try {
      uri = URI.create(location);
    } catch (IllegalArgumentException e) {
      String errorMessage = "Invalid Location specified, please specify a location in URI format.";
      throw new RobotRegistrationException(errorMessage + " " +  e.getLocalizedMessage(), e);
    }
    String scheme = uri.getScheme();
    if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
      scheme = "http";
    }
    String robotLocation;
    if (uri.getPort() != -1) {
      robotLocation = scheme + "://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
    } else {
      robotLocation = scheme + "://" + uri.getHost() + uri.getPath();
    }

    if (robotLocation.endsWith("/")) {
      robotLocation = robotLocation.substring(0, robotLocation.length() - 1);
    }

    // TODO(ljvderijk): Implement the verification.
    RobotAccountData robotAccount =
        new RobotAccountDataImpl(robotId, robotLocation,
            tokenGenerator.generateToken(TOKEN_LENGTH), null, true);
    accountStore.putAccount(robotAccount);
    LOG.info(robotAccount.getId() + " is now registered as a RobotAccount with Url "
        + robotAccount.getUrl());
    return robotAccount;
  }

  private RobotsUtil() {

  }
}
