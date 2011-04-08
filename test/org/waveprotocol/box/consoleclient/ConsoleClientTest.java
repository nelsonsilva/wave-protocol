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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.waveprotocol.box.server.util.testing.Matchers.contains;
import static org.waveprotocol.box.server.util.testing.Matchers.doesNotContain;

import junit.framework.TestCase;

import org.waveprotocol.box.common.IndexEntry;
import org.waveprotocol.box.common.IndexWave;
import org.waveprotocol.box.consoleclient.ScrollableWaveView.RenderMode;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.security.Permission;
import java.util.List;
import java.util.Set;

/**
 * Functional tests for the {@link ConsoleClient}.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class ConsoleClientTest extends TestCase implements TestingConstants {
  /** An exception to identify System.exit() calls */
  private static class ExitException extends SecurityException {
    public final int status;

    public ExitException(int status) {
      super("Exit with status " + status);
      this.status = status;
    }
  }

  /** The client under test */
  private ConsoleClient client;

  /** The backend of the client under test. Null if the client is not connected. */
  private ClientBackend backend = null;

  /** The testing utility for the client under test. Null if the client is not connected. */
  private ClientTestingUtil util = null;

  @Override
  public void setUp() throws IOException {
    client = new ConsoleClient(ClientTestingUtil.backendSpyFactory,
        ClientTestingUtil.getMockConsoleRenderer());
  }

  @Override
  public void tearDown() {
    client.disconnect();
  }

  // Tests

  /**
   * The client should be offline when created before anything is called. A quick test to avoid
   * untested assumptions in the other tests.
   */
  public void testStartsOffline() {
    assertFalse(client.isConnected());
  }

  /** "connect" command should connect the client to the correct user, server, and port */
  public void testConnectConnectsWithCorrectUser() {
    connect();

    assertTrue(client.isConnected());
    assertEquals(USER, backend.getUserId().getAddress());
  }

  /** "connect" command should open the index wave */
  public void testConnectOpensIndexWave() {
    connect();

    // Only the index wave should be open.
    assertEquals(1, util.getOpenWavesCount(true));
    assertNotNull(backend.getIndexWave());
    assertTrue(client.isInboxOpen());
    // No conversation wave should be displayed yet.
    assertFalse(client.isWaveOpen());
  }

  /** "new" command should create a single wave and open it, but not dislay it in the UI */
  public void testNewCreatesOneWaveAndOpensItButDoesNotDisplayIt() {
    connect();
    createNewWave();

    Set<WaveId> openWaves = util.getOpenWaveIds(true);
    ClientWaveView indexWave = backend.getIndexWave();

    // There should be 2 waves: the index wave and our new wave.
    assertEquals(2, openWaves.size());
    assertNotNull(indexWave);
    assertTrue(client.isInboxOpen());
    // The new wave should not be displayed yet (need to call "open" command to display it).
    assertFalse(client.isWaveOpen());

    // The index should contain only the new wave.
    List<IndexEntry> index = IndexWave.getIndexEntries(indexWave.getWavelets());
    assertEquals(1, index.size());
    WaveId newWaveId = index.get(0).getWaveId();
    assertThat(openWaves, contains(newWaveId));
  }

  /** "new" command should only add the user, and nothing else (no other users and no content) */
  public void testNewOnlyAddsSelf() {
    connect();
    createNewWave();

    ClientWaveView newWave = util.getFirstWave();
    String newWaveContent = util.getText(newWave);
    Set<ParticipantId> participants = util.getAllParticipants(newWave);

    assertEquals("", newWaveContent);
    assertEquals(1, participants.size());
    assertThat(participants, contains(backend.getUserId()));
  }

  /** "open" command should open a wave but not create new ones */
  public void testOpenHasNoSideEffects() {
    connect();
    createNewWave();

    // Make sure the wave is not open in the UI yet.
    assertFalse(client.isWaveOpen());

    int waveCount = util.getOpenWavesCount(true);
    // Open the wave in the UI (it should already be open in the backend).
    openWave(0);

    // The wave should now be open in UI.
    assertTrue(client.isWaveOpen());
    // No waves were created/deleted.
    assertEquals(waveCount, util.getOpenWavesCount(true));
  }

  /** "open" command should open correct wave */
  public void testOpenOpensCorrectWave() {
    connect();
    // We need at least 5 entries to cover all corner cases (1st, 2nd, last, before-last, middle).
    // We use 6 to be safe.
    for (int i=0; i<6; ++i) {
      createNewWave();
    }

    List<IndexEntry> index = IndexWave.getIndexEntries(backend.getIndexWave().getWavelets());
    for (int i=0; i < index.size(); ++i) {
      openWave(i);

      WaveId openedWaveId = client.getOpenWaveId();
      WaveId waveIdFromIndex = index.get(i).getWaveId();
      assertEquals(waveIdFromIndex, openedWaveId);
    }
  }

  /** "open" command should open the wave in normal display mode */
  public void testOpenOpensWaveInNormalDisplayMode() {
    connect();
    createNewWave();

    openWave(0);
    assertEquals(RenderMode.NORMAL, client.getRenderingMode());

    // Force xml mode.
    setViewMode("xml");
    // Check that the render mode really changed, so that we don't get a false
    // positive result below.
    assertEquals(RenderMode.XML, client.getRenderingMode());

    // Re-open the wave.
    openWave(0);
    // Should be in normal mode again.
    assertEquals(RenderMode.NORMAL, client.getRenderingMode());
  }

  /** "add" command should not add/delete/modify waves */
  public void testAddHasNoSideEffects() {
    connect();
    createNewWave();
    openWave(0);

    int oldWaveCount = util.getOpenWavesCount(true);
    WaveId oldOpenWaveId = client.getOpenWaveId();
    String oldContent = util.getText(oldOpenWaveId);

    addUser(OTHER_PARTICIPANT);

    int newWaveCount = util.getOpenWavesCount(true);
    WaveId newOpenWaveId = client.getOpenWaveId();
    String newContent = util.getText(newOpenWaveId);

    // No waves were created/deleted.
    assertEquals(oldWaveCount, newWaveCount);
    // The same wave should be open as before.
    assertEquals(oldOpenWaveId, newOpenWaveId);
    // The wave content was not changed.
    assertEquals(oldContent, newContent);
  }

  /** "add" command should add the specified user */
  public void testAddAddsUser() {
    connect();
    createNewWave();
    openWave(0);

    Set<ParticipantId> oldUsers = util.getAllParticipants(client.getOpenWaveId());

    addUser(OTHER_PARTICIPANT);

    Set<ParticipantId> newUsers = util.getAllParticipants(client.getOpenWaveId());

    assertThat(newUsers, contains(OTHER_PARTICIPANT));
    newUsers.remove(OTHER_PARTICIPANT);
    assertEquals(oldUsers, newUsers);
  }

  /** "remove" command should not add/delete/modify waves */
  public void testRemoveHasNoSideEffects() {
    connect();
    createNewWave();
    openWave(0);
    addUser(OTHER_PARTICIPANT);

    int oldWaveCount = util.getOpenWavesCount(true);
    WaveId oldOpenWaveId = client.getOpenWaveId();
    String oldContent = util.getText(oldOpenWaveId);

    removeUser(OTHER_PARTICIPANT);

    int newWaveCount = util.getOpenWavesCount(true);
    WaveId newOpenWaveId = client.getOpenWaveId();
    String newContent = util.getText(newOpenWaveId);

    // No waves were created/deleted.
    assertEquals(oldWaveCount, newWaveCount);
    // The same wave should be open as before.
    assertEquals(oldOpenWaveId, newOpenWaveId);
    // The wave content was not changed.
    assertEquals(oldContent, newContent);
  }

  /** "remove" command should remove the specified user */
  public void testRemoveRemovesUser() {
    connect();
    createNewWave();
    openWave(0);
    addUser(OTHER_PARTICIPANT);

    Set<ParticipantId> oldUsers = util.getAllParticipants(client.getOpenWaveId());

    removeUser(OTHER_PARTICIPANT);

    Set<ParticipantId> newUsers = util.getAllParticipants(client.getOpenWaveId());

    assertThat(newUsers, doesNotContain(OTHER_PARTICIPANT));
    newUsers.add(OTHER_PARTICIPANT);
    assertEquals(oldUsers, newUsers);
  }

  /** "read" command should mark all known waves as read */
  public void testReadMarksAllWavesAsRead() {
    connect();
    for (int i=0; i<3; ++i) {
      createNewWave();
    }

    List<IndexEntry> index = IndexWave.getIndexEntries(backend.getIndexWave().getWavelets());

    // Check that all waves are unread.
    for (int i=0; i < index.size(); ++i) {
      assertFalse(isRead(i));
    }

    markAllRead();

    // All waves should now be read.
    for (int i=0; i < index.size(); ++i) {
      assertTrue(isRead(i));
    }
  }

  /** "scroll" command  should set the correct scrolling step */
  public void testScrollSetsCorrectScrollingStep() {
    int oldStep = client.getScrollLines();
    int newStep = oldStep + 3;

    setScrollStep(newStep);

    assertEquals(newStep, client.getScrollLines());
  }

  /** "view" command should set the correct render mode */
  public void testViewSetsCorrectRenderMode() {
    connect();
    createNewWave();
    openWave(0);

    assertEquals(RenderMode.NORMAL, client.getRenderingMode());

    setViewMode("xml");
    assertEquals(RenderMode.XML, client.getRenderingMode());

    setViewMode("normal");
    assertEquals(RenderMode.NORMAL, client.getRenderingMode());
  }

  /** "quit" command should clean up and shut down the client */
  public void testQuitExitsCleanly() {
    connect();
    createNewWave();
    openWave(0);

    // Set up a custom security manager to intercept System.exit() calls.
    // Taken from http://stackoverflow.com/questions/309396/java-how-to-test-methods-that-call-system-exit
    SecurityManager oldSecurityManager = System.getSecurityManager();
    System.setSecurityManager(new SecurityManager() {
      @Override
      public void checkPermission(Permission perm) {
        // Allow anything.
      }
      @Override
      public void checkPermission(Permission perm, Object context) {
        // Allow anything.
      }
      @Override
      public void checkExit(int status) {
        throw new ExitException(status);
      }
    });

    int exitStatus = -1;
    try {
      quit();
    } catch (ExitException e) {
      exitStatus = e.status;
    } finally {
      System.setSecurityManager(oldSecurityManager);

      // The client should exit without an error.
      assertEquals(0, exitStatus);
      // The client should have disconnected.
      assertFalse(client.isConnected());
    }
  }

  /** A simple text message should be added to the wave as is, with no other side effects */
  public void testTextMessageIsAddedWithNoSideEffects() {
    connect();
    createNewWave();
    openWave(0);

    int oldWaveCount = util.getOpenWavesCount(true);
    WaveId oldOpenWaveId = client.getOpenWaveId();
    String oldContent = util.getText(oldOpenWaveId);
    Set<ParticipantId> oldUsers = util.getAllParticipants(oldOpenWaveId);

    assertEquals("", oldContent);

    postText(MESSAGE);

    int newWaveCount = util.getOpenWavesCount(true);
    WaveId newOpenWaveId = client.getOpenWaveId();
    String newContent = util.getText(newOpenWaveId);
    Set<ParticipantId> newUsers = util.getAllParticipants(newOpenWaveId);

    // No waves were created/deleted.
    assertEquals(oldWaveCount, newWaveCount);
    // The same wave should be open as before.
    assertEquals(oldOpenWaveId, newOpenWaveId);
    // No users were added/deleted.
    assertEquals(oldUsers, newUsers);
    // The wave text should contain the message and nothing else.
    assertEquals(MESSAGE, newContent);
  }

  // Utiltity methods

  /** Add the specified user to the current wave (run the "/add" command) */
  private void addUser(ParticipantId user) {
    assertTrue(client.isConnected());
    assertTrue(client.isWaveOpen());
    client.processLine("/add " + user.getAddress());
    backend.waitForAccumulatedEventsToProcess();
  }

  /** Connect the client (run the "/connect" command) */
  private void connect() {
    assertFalse(client.isConnected());
    client.processLine(String.format("/connect %s %s:%d %s", USER, DOMAIN, PORT, PASSWORD));
    client.getBackend().waitForAccumulatedEventsToProcess();

    // We can now use the client backend and testing utility for the rest of the test:
    backend = client.getBackend();
    util = new ClientTestingUtil(backend);
  }

  /** Create a new wave (run the "/new" command) */
  private void createNewWave() {
    assertTrue(client.isConnected());
    client.processLine("/new");
    backend.waitForAccumulatedEventsToProcess();
  }

  /**
   * Checks that the latest change to the specified inbox entry have been read.
   *
   * @param entry the number (zero-based) of the inbox entry to check
   * @return true if the two versions are the same, or false otherwise
   */
  private boolean isRead(int entry) {
    List<IndexEntry> index = IndexWave.getIndexEntries(backend.getIndexWave().getWavelets());
    ClientWaveView wave = backend.getWave(index.get(entry).getWaveId());
    return client.isRead(wave);
  }

  /** Mark all waves as read (run the "/read" command) */
  private void markAllRead() {
    assertTrue(client.isConnected());
    client.processLine("/read");
    backend.waitForAccumulatedEventsToProcess();
  }

  /**
   * Open the specified wave (run the "/open" command).
   *
   * @param entry number of the wave in the index (zero-based).
   */
  private void openWave(int entry) {
    assertTrue(client.isConnected());
    client.processLine("/open " + entry);
    backend.waitForAccumulatedEventsToProcess();
  }

  /** Enter a text message in the current wave */
  private void postText(String message) {
    // Check that it's not a command and the client is connected and has an open wave.
    assertFalse(message.startsWith("/"));
    assertTrue(client.isConnected());
    assertTrue(client.isWaveOpen());

    client.processLine(message);
    backend.waitForAccumulatedEventsToProcess();
  }

  /** Remove the specified user from the current wave (run the "/remove" command) */
  private void removeUser(ParticipantId user) {
    assertTrue(client.isConnected());
    assertTrue(client.isWaveOpen());
    client.processLine("/remove " + user.getAddress());
    backend.waitForAccumulatedEventsToProcess();
  }

  /** Sets the scrolling step (runs the "/scroll" command) */
  private void setScrollStep(int lines) {
    client.processLine("/scroll " + lines);
  }

  /** Sets the view to the specified mode (runs the "/view" command) */
  private void setViewMode(String viewMode) {
    assertTrue(client.isConnected());
    assertTrue(client.isWaveOpen());
    client.processLine("/view " + viewMode);
  }

  /** Quit the client (run the "/quit" command) */
  private void quit() {
    client.processLine("/quit");
  }
}
