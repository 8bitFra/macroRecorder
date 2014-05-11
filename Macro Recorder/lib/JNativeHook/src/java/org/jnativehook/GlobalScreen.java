/* JNativeHook: Global keyboard and mouse hooking for Java.
 * Copyright (C) 2006-2014 Alexander Barker.  All Rights Received.
 * http://code.google.com/p/jnativehook/
 * 
 * JNativeHook is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JNativeHook is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jnativehook;

// Imports.
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;
import org.jnativehook.mouse.NativeMouseMotionListener;
import org.jnativehook.mouse.NativeMouseWheelEvent;
import org.jnativehook.mouse.NativeMouseWheelListener;

import javax.swing.event.EventListenerList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EventListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * GlobalScreen is used to represent the native screen area that Java does not
 * usually have access to. This class can be thought of as the source component
 * for native input events.
 * <p/>
 * This class also handles the loading, unpacking and communication with the
 * native library. That includes registering and un-registering the native hook
 * with the underlying operating system and adding global keyboard and mouse
 * listeners.
 *
 * @author Alexander Barker (<a href="mailto:alex@1stleg.com">alex@1stleg.com</a>)
 * @version 1.1
 */
public class GlobalScreen {
	/**
	 * The GlobalScreen singleton.
	 */
	private static final GlobalScreen instance = new GlobalScreen();

	/**
	 * The list of event listeners to notify.
	 */
	private static final EventListenerList eventListeners = new EventListenerList();

	/**
	 * The service to dispatch events.
	 */
	private static ExecutorService eventExecutor;

	/**
	 * Private constructor to prevent multiple instances of the global screen.
	 * The {@link #registerNativeHook} method will be called on construction to
	 * unpack and load the native library.
	 */
	private GlobalScreen() {
		// Unpack and Load the native library.
		GlobalScreen.loadNativeLibrary();

		GlobalScreen.eventExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("JNativeHook Native Dispatch");

				return t;
			}
		});
	}

	/**
	 * A destructor that will perform native cleanup by calling the
	 * {@link #unregisterNativeHook} method.  This method will not run until the
	 * class is garbage collected.
	 *
	 * @throws Throwable a <code>NativeHookException</code> raised by calling
	 *                   {@link #unloadNativeLibrary()}
	 * @see Object#finalize
	 */
	@Override
	protected void finalize() throws Throwable {
		if (GlobalScreen.isNativeHookRegistered()) {
			GlobalScreen.unloadNativeLibrary();
		}

		// Shutdown the current Event executor.
		eventExecutor.shutdownNow();
		eventExecutor = null;

		super.finalize();
	}

	/**
	 * Returns the singleton instance of <code>GlobalScreen</code>.
	 *
	 * @return singleton instance of <code>GlobalScreen</code>
	 */
	public static synchronized GlobalScreen getInstance() {
		return GlobalScreen.instance;
	}

	/**
	 * Adds the specified native key listener to receive key events from the
	 * native system. If listener is null, no exception is thrown and no action
	 * is performed.
	 *
	 * @param listener a native key listener object
	 */
	public void addNativeKeyListener(NativeKeyListener listener) {
		if (listener != null) {
			eventListeners.add(NativeKeyListener.class, listener);
		}
	}

	/**
	 * Removes the specified native key listener so that it no longer receives
	 * key events from the native system. This method performs no function if
	 * the listener specified by the argument was not previously added.  If
	 * listener is null, no exception is thrown and no action is performed.
	 *
	 * @param listener a native key listener object
	 */
	public void removeNativeKeyListener(NativeKeyListener listener) {
		if (listener != null) {
			eventListeners.remove(NativeKeyListener.class, listener);
		}
	}

	/**
	 * Adds the specified native mouse listener to receive mouse events from the
	 * native system. If listener is null, no exception is thrown and no action
	 * is performed.
	 *
	 * @param listener a native mouse listener object
	 */
	public void addNativeMouseListener(NativeMouseListener listener) {
		if (listener != null) {
			eventListeners.add(NativeMouseListener.class, listener);
		}
	}

	/**
	 * Removes the specified native mouse listener so that it no longer receives
	 * mouse events from the native system. This method performs no function if
	 * the listener specified by the argument was not previously added.  If
	 * listener is null, no exception is thrown and no action is performed.
	 *
	 * @param listener a native mouse listener object
	 */
	public void removeNativeMouseListener(NativeMouseListener listener) {
		if (listener != null) {
			eventListeners.remove(NativeMouseListener.class, listener);
		}
	}

	/**
	 * Adds the specified native mouse motion listener to receive mouse motion
	 * events from the native system. If listener is null, no exception is
	 * thrown and no action is performed.
	 *
	 * @param listener a native mouse motion listener object
	 */
	public void addNativeMouseMotionListener(NativeMouseMotionListener listener) {
		if (listener != null) {
			eventListeners.add(NativeMouseMotionListener.class, listener);
		}
	}

	/**
	 * Removes the specified native mouse motion listener so that it no longer
	 * receives mouse motion events from the native system. This method performs
	 * no function if the listener specified by the argument was not previously
	 * added.  If listener is null, no exception is thrown and no action is
	 * performed.
	 *
	 * @param listener a native mouse motion listener object
	 */
	public void removeNativeMouseMotionListener(NativeMouseMotionListener listener) {
		if (listener != null) {
			eventListeners.remove(NativeMouseMotionListener.class, listener);
		}
	}

	/**
	 * Adds the specified native mouse wheel listener to receive mouse wheel
	 * events from the native system. If listener is null, no exception is
	 * thrown and no action is performed.
	 *
	 * @param listener a native mouse wheel listener object
	 * @since 1.1
	 */
	public void addNativeMouseWheelListener(NativeMouseWheelListener listener) {
		if (listener != null) {
			eventListeners.add(NativeMouseWheelListener.class, listener);
		}
	}

	/**
	 * Removes the specified native mouse wheel listener so that it no longer
	 * receives mouse wheel events from the native system. This method performs
	 * no function if the listener specified by the argument was not previously
	 * added.  If listener is null, no exception is thrown and no action is
	 * performed.
	 *
	 * @param listener a native mouse wheel listener object
	 * @since 1.1
	 */
	public void removeNativeMouseWheelListener(NativeMouseWheelListener listener) {
		if (listener != null) {
			eventListeners.remove(NativeMouseWheelListener.class, listener);
		}
	}

	/**
	 * Enable the native hook if it is not currently running. If it is running
	 * the function has no effect.
	 * <p/>
	 * <b>Note:</b> This method will throw a <code>NativeHookException</code>
	 * if specific operating system features are unavailable or disabled.
	 * For example: Access for assistive devices is unchecked in the Universal
	 * Access section of the System Preferences on Apple's OS X platform or
	 * <code>Load "record"</code> is missing for the xorg.conf file on
	 * Unix/Linux/Solaris platforms.
	 *
	 * @throws NativeHookException problem registering the native hook with
	 *                             the underlying operating system.
	 * @since 1.1
	 */
	public static native void registerNativeHook() throws NativeHookException;

	/**
	 * Disable the native hook if it is currently registered. If the native
	 * hook it is not registered the function has no effect.
	 *
	 * @since 1.1
	 */
	public static native void unregisterNativeHook();

	/**
	 * Returns <code>true</code> if the native hook is currently registered.
	 *
	 * @return true if the native hook is currently registered.
	 * @throws NativeHookException the native hook exception
	 * @since 1.1
	 */
	public static native boolean isNativeHookRegistered();

	/**
	 * Send a native input event to the system.
	 *
	 * @since 1.2
	 */
	public static native void postNativeEvent(NativeInputEvent e);

	/**
	 * Dispatches an event to the appropriate processor.  This method is
	 * generally called by the native library but maybe used to synthesize
	 * native events from Java.
	 * <p/>
	 * <b>Note:</b> This method executes on the native systems event queue.
	 * It is imperative that all processing be off-loaded to other threads.
	 * Failure to do so may result in the delay of user input and the automatic
	 * removal of the native hook.
	 */
	public final void dispatchEvent(NativeInputEvent e) {
		if (eventExecutor != null) {
			if (e instanceof NativeKeyEvent) {
				processKeyEvent((NativeKeyEvent) e);
			}
			else if (e instanceof NativeMouseWheelEvent) {
				processMouseWheelEvent((NativeMouseWheelEvent) e);
			}
			else if (e instanceof NativeMouseEvent) {
				processMouseEvent((NativeMouseEvent) e);
			}
		}
	}

	/**
	 * Processes native key events by dispatching them to all registered
	 * <code>NativeKeyListener</code> objects.
	 *
	 * @param e the <code>NativeKeyEvent</code> to dispatch.
	 * @see NativeKeyEvent
	 * @see NativeKeyListener
	 * @see #addNativeKeyListener(NativeKeyListener)
	 */
	private void processKeyEvent(NativeKeyEvent e) {
		/*
		try {
			Field f = NativeInputEvent.class.getDeclaredField("propagate");
			f.setAccessible(true);
			f.setBoolean(e, false);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		*/

		// The event cannot be modified beyond this point!  This is both a 
		// Java restriction and a native code restriction.
		final NativeKeyEvent event = e;
		eventExecutor.execute(new Runnable() {
			public void run() {
				int id = event.getID();
				EventListener[] listeners = eventListeners.getListeners(NativeKeyListener.class);

				for (int i = 0; i < listeners.length; i++) {
					switch (id) {
						case NativeKeyEvent.NATIVE_KEY_PRESSED:
							((NativeKeyListener) listeners[i]).nativeKeyPressed(event);
							break;

						case NativeKeyEvent.NATIVE_KEY_TYPED:
							((NativeKeyListener) listeners[i]).nativeKeyTyped(event);
							break;

						case NativeKeyEvent.NATIVE_KEY_RELEASED:
							((NativeKeyListener) listeners[i]).nativeKeyReleased(event);
							break;
					}
				}
			}
		});
	}

	/**
	 * Processes native mouse events by dispatching them to all registered
	 * <code>NativeMouseListener</code> objects.
	 *
	 * @param e the <code>NativeMouseEvent</code> to dispatch.
	 * @see NativeMouseEvent
	 * @see NativeMouseListener
	 * @see #addNativeMouseListener(NativeMouseListener)
	 */
	private void processMouseEvent(NativeMouseEvent e) {
		// The event cannot be modified beyond this point!  This is both a 
		// Java restriction and a native code restriction.
		final NativeMouseEvent event = e;
		eventExecutor.execute(new Runnable() {
			public void run() {
				int id = event.getID();

				EventListener[] listeners;
				if (id == NativeMouseEvent.NATIVE_MOUSE_MOVED || id == NativeMouseEvent.NATIVE_MOUSE_DRAGGED) {
					listeners = eventListeners.getListeners(NativeMouseMotionListener.class);
				}
				else {
					listeners = eventListeners.getListeners(NativeMouseListener.class);
				}

				for (int i = 0; i < listeners.length; i++) {
					switch (id) {
						case NativeMouseEvent.NATIVE_MOUSE_CLICKED:
							((NativeMouseListener) listeners[i]).nativeMouseClicked(event);
							break;

						case NativeMouseEvent.NATIVE_MOUSE_PRESSED:
							((NativeMouseListener) listeners[i]).nativeMousePressed(event);
							break;

						case NativeMouseEvent.NATIVE_MOUSE_RELEASED:
							((NativeMouseListener) listeners[i]).nativeMouseReleased(event);
							break;

						case NativeMouseEvent.NATIVE_MOUSE_MOVED:
							((NativeMouseMotionListener) listeners[i]).nativeMouseMoved(event);
							break;

						case NativeMouseEvent.NATIVE_MOUSE_DRAGGED:
							((NativeMouseMotionListener) listeners[i]).nativeMouseDragged(event);
							break;
					}
				}
			}
		});
	}

	/**
	 * Processes native mouse wheel events by dispatching them to all registered
	 * <code>NativeMouseWheelListener</code> objects.
	 *
	 * @param e The <code>NativeMouseWheelEvent</code> to dispatch.
	 * @see NativeMouseWheelEvent
	 * @see NativeMouseWheelListener
	 * @see #addNativeMouseWheelListener(NativeMouseWheelListener)
	 * @since 1.1
	 */
	private void processMouseWheelEvent(NativeMouseWheelEvent e) {
		// The event cannot be modified beyond this point!  This is both a 
		// Java restriction and a native code restriction.
		final NativeMouseWheelEvent event = e;
		eventExecutor.execute(new Runnable() {
			public void run() {
				EventListener[] listeners = eventListeners.getListeners(NativeMouseWheelListener.class);

				for (int i = 0; i < listeners.length; i++) {
					((NativeMouseWheelListener) listeners[i]).nativeMouseWheelMoved(event);
				}
			}
		});
	}

	/**
	 * Set a different executor service for native event delivery.  By default,
	 * JNativeHook utilizes a single thread executor to dispatch events from
	 * the native event queue.  You may choose to use an alternative approach
	 * for event delivery by implementing an <code>ExecutorService</code>.
	 * <p/>
	 * <b>Note:</b> Using null as an <code>ExecutorService</code> will cause all
	 * delivered events to be discard until a valid <code>ExecutorService</code>
	 * is set.
	 *
	 * @param dispatcher The <code>ExecutorService</code> used to dispatch native events.
	 * @see java.util.concurrent.ExecutorService
	 * @see java.util.concurrent.Executors#newSingleThreadExecutor()
	 * @since 1.2
	 */
	public final void setEventDispatcher(ExecutorService dispatcher) {
		if (GlobalScreen.eventExecutor != null) {
			GlobalScreen.eventExecutor.shutdown();
		}

		GlobalScreen.eventExecutor = dispatcher;
	}

	/**
	 * Perform procedures to interface with the native library. These procedures
	 * include unpacking and loading the library into the Java Virtual Machine.
	 */
	private static void loadNativeLibrary() {
		System.out.println("\n" +
			"JNativeHook: Global keyboard and mouse hooking for Java.\n" +
			"Copyright (C) 2006-2014 Alexander Barker.  All Rights Received.\n" +
			"https://github.com/kwhat/jnativehook/\n" +
			"\n" +
			"JNativeHook is free software: you can redistribute it and/or modify\n" +
			"it under the terms of the GNU Lesser General Public License as published\n" +
			"by the Free Software Foundation, either version 3 of the License, or\n" +
			"(at your option) any later version.\n" +
			"\n" +
			"JNativeHook is distributed in the hope that it will be useful,\n" +
			"but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
			"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
			"GNU General Public License for more details.\n" +
			"\n" +
			"You should have received a copy of the GNU Lesser General Public License\n" +
			"along with this program.  If not, see <http://www.gnu.org/licenses/>.\n");

		String libName = "JNativeHook";

		try {
			// Try to load the native library assuming the java.library.path was
			// set correctly at launch.
			System.loadLibrary(libName);
		}
		catch (UnsatisfiedLinkError linkError) {
			// The library is not in the java.library.path so try to extract it.
			try {
				String libResourcePath = "/org/jnativehook/lib/" +
						NativeSystem.getFamily() + "/" +
						NativeSystem.getArchitecture() + "/";

				// Get what the system "thinks" the library name should be.
				String libNativeName = System.mapLibraryName(libName);
				// Hack for OS X JRE 1.6 and earlier.
				libNativeName = libNativeName.replaceAll("\\.jnilib$", "\\.dylib");

				// Slice up the library name.
				int i = libNativeName.lastIndexOf('.');
				String libNativePrefix = libNativeName.substring(0, i) + '_';
				String libNativeSuffix = libNativeName.substring(i);

				// Determine if the user specified temp directory should be used.
				String tmpDir = System.getProperty("jnativehook.tmpdir", null);

				File libDir = null;
				if (tmpDir != null) {
					libDir = new File(tmpDir);
				}

				// Create the temp file for this instance of the library.
				File libFile = File.createTempFile(libNativePrefix, libNativeSuffix, libDir);

				// This may return null in some circumstances.
				InputStream libInputStream =
						GlobalScreen.class.getResourceAsStream(
								libResourcePath.toLowerCase()
										+ libNativeName
						);

				if (libInputStream == null) {
					throw new IOException("Unable to locate the native library.");
				}

				// Check and see if a copy of the native lib already exists.
				FileOutputStream libOutputStream = new FileOutputStream(libFile);
				byte[] buffer = new byte[4 * 1024];

				int size;
				while ((size = libInputStream.read(buffer)) != -1) {
					libOutputStream.write(buffer, 0, size);
				}
				libOutputStream.close();
				libInputStream.close();

				libFile.deleteOnExit();

				System.load(libFile.getPath());
			}
			catch (IOException e) {
				// Tried and Failed to manually setup the java.library.path.
				throw new RuntimeException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Perform procedures to cleanup the native library. This method is called
	 * on garbage collection to ensure proper native cleanup.
	 */
	private static void unloadNativeLibrary() throws NativeHookException {
		// Make sure the native thread has stopped.
		GlobalScreen.unregisterNativeHook();
	}
}
