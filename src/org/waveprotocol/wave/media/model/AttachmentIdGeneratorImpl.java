/**
 * Copyright 2009 Google Inc.
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
package org.waveprotocol.wave.media.model;

import org.waveprotocol.wave.model.id.IdGenerator;

/**
 * This class is used to generate Attachment ids.
 *
 */
public class AttachmentIdGeneratorImpl implements AttachmentIdGenerator {

  private IdGenerator idGenerator;

  public AttachmentIdGeneratorImpl(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  @Override
  public AttachmentId newAttachmentId() {
    return new AttachmentId(idGenerator.getDefaultDomain(), idGenerator.newUniqueToken());
  }

  @Override
  public AttachmentId newAttachmentId(String overrideDomain) {
    return new AttachmentId(overrideDomain,  idGenerator.newUniqueToken());
  }
}
