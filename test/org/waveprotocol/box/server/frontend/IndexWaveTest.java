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

package org.waveprotocol.box.server.frontend;

import static org.waveprotocol.box.common.CommonConstants.INDEX_WAVE_ID;
import static org.waveprotocol.wave.model.id.IdConstants.CONVERSATION_ROOT_WAVELET;

import junit.framework.TestCase;

import org.waveprotocol.box.common.IndexWave;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Tests for the {@link IndexWave}. The tests were migrated from ClientFrontendImplTest.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class IndexWaveTest extends TestCase {
  private static final WaveId WAVE_ID = WaveId.of("domain", "waveId");
  private static final WaveletId WAVELET_ID = WaveletId.of("domain", CONVERSATION_ROOT_WAVELET);
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);
  private static final WaveletName INDEX_WAVELET_NAME = WaveletName.of(INDEX_WAVE_ID,
      WaveletId.of("domain", "waveId"));

  /**
   * Test that a wavelet name can be converted to the corresponding index
   * wavelet name if and only if the wavelet's domain equals the wave's domain
   * and the wavelet's ID is CONVERSATION_ROOT_WAVELET.
   */
  public void testIndexWaveletNameConversion() {
    assertTrue(IndexWave.canBeIndexed(WAVELET_NAME));
    assertTrue(IndexWave.canBeIndexed(WAVE_ID));

    assertEquals(INDEX_WAVELET_NAME, IndexWave.indexWaveletNameFor(WAVE_ID));
    assertEquals(WAVE_ID, IndexWave.waveIdFromIndexWavelet(INDEX_WAVELET_NAME));

    // The wavelet is part of an index wave:
    try {
      assertFalse(IndexWave.canBeIndexed(INDEX_WAVE_ID));
      IndexWave.indexWaveletNameFor(INDEX_WAVE_ID);
      fail("Should have thrown IllegalArgumentException - index wave wavelets shouldn't be "
          + "convertable to index wave");
    } catch (IllegalArgumentException expected) {
      // pass
    }
  }
}
