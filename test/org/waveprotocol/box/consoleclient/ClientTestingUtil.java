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

package org.waveprotocol.box.consoleclient;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.waveprotocol.box.common.DocumentConstants.MANIFEST_DOCUMENT_ID;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.waveprotocol.box.common.Snippets;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.box.server.util.BlockingSuccessFailCallback;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.client.common.util.ClientPercentEncoderDecoder;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperationVisitor;
import org.waveprotocol.wave.model.operation.wave.BlipOperationVisitorImpl;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for tesing the client and related classes.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class ClientTestingUtil {

  /**
   * Timeout, in milliseconds, for tests that may fail through abnormal
   * behaviors such as deadlocks or infinite loops. Usually 1000-2000 ms should
   * be enough. We give a little more to be safe.
   */
  public static final long TEST_TIMEOUT = 5000;

  public static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new ClientPercentEncoderDecoder());
  public static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);

  /**
   * ClientBackend factory that creates a spy object on the backend and injects a fake RPC
   * objects factory.
   */
  public static final ClientBackend.Factory backendSpyFactory = new ClientBackend.Factory() {
      @Override
      public ClientBackend create(String userAtDomain, String server)
          throws IOException {
        ClientAuthenticator authenticator = mock(ClientAuthenticator.class);
        HttpCookie cookie = new HttpCookie("anything", "token");
        when(authenticator.authenticate(anyString(), any(char[].class))).thenReturn(cookie);

        return spy(new ClientBackend(userAtDomain, server, new FakeRpcObjectFactory(),
            HASH_FACTORY, authenticator));
      }
    };

  /**
   * @return a new mock renderer for the console client. The renderer skips rendering to speed up
   * the tests and allows checking if the render method was called.
   */
  // TODO(Michael): Add checks to the ConsoleClientTest to check that render is being called when
  // appropriate.
  public static ConsoleClient.Renderer getMockConsoleRenderer() {
    return mock(ConsoleClient.Renderer.class);
  }

  /** The client backend on which this util instance acts. */
  private final ClientBackend backend;

  /** The user stored in the backend */
  private final ParticipantId userId;

  /**
   * Constructs a {@code ClientTestingUtil} that acts on the given client backend.
   *
   * @param backend to act on.
   */
  public ClientTestingUtil(ClientBackend backend) {
    this.backend = backend;
    userId = backend.getUserId();
  }

  /**
   * Verifies that an operation completed without errors within the time set by the test timeout.
   *
   * @param callback the blocking callback that was used for the operation.
   */
  public void assertOperationComplete(
      BlockingSuccessFailCallback<ProtocolSubmitResponse, String> callback) {
    // Make sure the test times out if something is wrong.
    final long waitTimeout = TEST_TIMEOUT * 2;
    final TimeUnit waitUnit = TimeUnit.MILLISECONDS;

    Pair<ProtocolSubmitResponse, String> result = callback.await(waitTimeout, waitUnit);
    // Process any incoming events that may have been generated.
    backend.waitForAccumulatedEventsToProcess();

    assertNotNull(result);
    assertNotNull(result.getFirst());
  }

  /**
   * Creates a new empty wavelet. The wavelet is not part of a {@code
   * ClientWaveView} and not stored in the client backend.
   */
  public WaveletData createWavelet() throws OperationException {
    return createWavelet(WaveletName.of("example.com", "wave", "example.com", "wavelet"), userId);
  }

  /**
   * Creates a new empty wavelet with an empty manifest document and the
   * specified wavelet name. The wavelet is not part of a {@code ClientWaveView}
   * and not stored in the client backend.
   *
   * @param waveletName of the new wavelet.
   * @param creator the id of the wavelet creator
   * @return the new wavelet
   */
  public WaveletData createWavelet(WaveletName waveletName, ParticipantId creator)
      throws OperationException {
    long dummyCreationTime = System.currentTimeMillis();
    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(waveletName, creator,
        HashedVersion.unsigned(0), dummyCreationTime);
    BlipData manifest = WaveletDataUtil.addEmptyBlip(wavelet, MANIFEST_DOCUMENT_ID, creator, 0L);
    manifest.getContent().consume(ClientUtils.createManifest());
    return wavelet;
  }

  /**
   * Creates a valid wave (and wavelet) in the client backend.
   *
   * @return the new wave's conversation root wavelet.
   */
  public WaveletData createWaveletInBackend() {
    BlockingSuccessFailCallback<ProtocolSubmitResponse, String> callback =
        BlockingSuccessFailCallback.create();
    ClientWaveView view = backend.createConversationWave(callback);
    // Make sure the wavelet creation completes successfully before returning the wavelet.
    assertOperationComplete(callback);
    Preconditions.checkNotNull(ClientUtils.getConversationRoot(view), "Wavelet creation failed");
    return ClientUtils.getConversationRoot(view);
  }

  /**
   * Returns all documents in the wave, aggregated from all the wavelets.
   *
   * @param wave to get the documents from.
   * @return map of all documents in the wave, aggregated from all the wavelets, and keyed by their
   * IDs.
   */
  public Map<String, BlipData> getAllDocuments(ClientWaveView wave) {
    return ClientUtils.getAllDocuments(wave);
  }

  /**
   * Returns all documents in the wave, aggregated from all the wavelets. The wave is retrieved
   * from the client backend using the given wave ID.
   *
   * @param waveId of the wave to get the documents from.
   * @return map of all documents in the wave, aggregated from all the wavelets, and keyed by their
   * IDs.
   */
  public Map<String, BlipData> getAllDocuments(WaveId waveId) {
    return getAllDocuments(backend.getWave(waveId));
  }

  /**
   * Returns all participants in the wave, aggregated from all the wavelets.
   *
   * @param wave to get the participants from.
   * @return all participants in the wave, aggregated from all the wavelets.
   */
  public Set<ParticipantId> getAllParticipants(ClientWaveView wave) {
    return ClientUtils.getAllParticipants(wave);
  }

  /**
   * Returns all participants in the wave, aggregated from all the wavelets. The wave is retrieved
   * from the client backend using the given wave ID.
   *
   * @param waveId of the wave to get the participants from.
   * @return all participants in the wave, aggregated from all the wavelets.
   */
  public Set<ParticipantId> getAllParticipants(WaveId waveId) {
    return getAllParticipants(backend.getWave(waveId));
  }

  /**
   * @return the first open wave in the client backend, not counting the index wave.
   */
  public ClientWaveView getFirstWave() {
    return backend.getWave(getFirstWaveId());
  }

  /**
   * @return the WaveId of the first open wave in the client backend, not counting the index wave.
   */
  public WaveId getFirstWaveId() {
    return getOpenWaveId(0, false);
  }

  /**
   * Returns the WaveId of the n-th open wave in the client backend.
   *
   * @param index of the open wave whose id to retreive (zero based).
   * @param includingIndexWave should the index wave be included in the count of open waves?
   * @return the WaveId, or null if not found.
   */
  public WaveId getOpenWaveId(int index, boolean includingIndexWave) {
    for (WaveId waveId : getOpenWaveIds(includingIndexWave)) {
      if (index == 0) {
        return waveId;
      }
      --index;
    }

    // Wave not found (index is out of range).
    return null;
  }

  /**
   * Returns the set of wave IDs of the waves that are currently open in the client backend,
   * optionally including the index wave.
   *
   * @param includeIndexWave should the index wave be included in the returned set?
   * @return the set of wave Ids of the open waves.
   */
  public Set<WaveId> getOpenWaveIds(boolean includeIndexWave) {
    return backend.getOpenWaveIds(includeIndexWave);
  }

  /**
   * Counts the waves curretly open in the client backend.
   *
   * @param includeIndexWave should the index wave be included in the count?
   * @return the number of waves currently open in the client backend.
   */
  public int getOpenWavesCount(boolean includeIndexWave) {
    return getOpenWaveIds(includeIndexWave).size();
  }

  /**
   * Collects the text from the specified blip document.
   *
   * @param blip document to collect the text from.
   * @return A string containing the characters from the blip.
   */
  public static String getText(ReadableBlipData blip) {
    return Snippets.collateTextForDocuments(Lists.newArrayList(blip));
  }

  /**
   * Collates the specified document operations into a string equivalent to the resulting wavelet
   * content.
   *
   * @param ops to collate.
   * @return the resulting text content.
   */
  public String getText(List<WaveletBlipOperation> ops) {
    final List<DocOp> docOps = Lists.newArrayList();
    BlipOperationVisitor opVisitor = new BlipOperationVisitorImpl() {
      @Override
      public void visitBlipContentOperation(BlipContentOperation op) {
        docOps.add(op.getContentOp());
      }
    };
    for (WaveletBlipOperation op : ops) {
      // Skip changes to the manifest document since they may contain "retain"
      // and other components that can't be collated by ClientUtils, and we
      // don't really care about the manifest anyway.
      if (!op.getBlipId().equals(MANIFEST_DOCUMENT_ID)) {
        op.getBlipOp().acceptVisitor(opVisitor);
      }
    }
    return Snippets.collateTextForOps(docOps);
  }

  /**
   * Collects the text of all of the documents in a wave into a single String.
   *
   * @param wave wave to collect the text from.
   * @return the collected text from the wave.
   */
  public String getText(ClientWaveView wave) {
    return ClientUtils.collateText(wave);
  }

  /**
   * Collects the text of all of the documents in a wave into a single String. The wave is
   * retrieved from the client backend using the given wave ID.
   *
   * @param waveId of the wave to collect the text from.
   * @return the collected text from the wave.
   */
  public String getText(WaveId waveId) {
    return getText(backend.getWave(waveId));
  }
}
