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

package org.waveprotocol.box.server.robots.operations;

import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;

import javax.annotation.Nullable;

/**
 * Helper class with utility methods.
 * 
 * @author vega113@gmail.com (Yuri Z.)
 */
public final class SearchHelper {
  
  private SearchHelper() {
    
  }
  
  /**
   * @return true if the wave has conversational root wavelet.
   */
  public static boolean hasConversationalRootWavelet(@Nullable WaveViewData wave) {
    if (wave == null) {
      return false;
    }
    for (ObservableWaveletData waveletData : wave.getWavelets()) {
      WaveletId waveletId = waveletData.getWaveletId();
      if (IdUtil.isConversationRootWaveletId(waveletId)) {
        return true;
      }
    }
    return false;
  }
}
