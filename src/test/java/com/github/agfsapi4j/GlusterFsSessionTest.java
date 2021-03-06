package com.github.agfsapi4j;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GlusterFsSessionTest
{
	private static final long CONNECTED_SESSION_ID = 1;

	private static final long OPENED_FILE_ID = 2;

	@InjectMocks
	private GlusterFsSession session;

	@Mock
	private LogAccess logAccess;

	@Mock
	private ResourceTracker resourceTracker;

	@Mock
	private LibGfapi lib;

	@BeforeClass
	public static void setUp()
	{
		LibGfapiProvider.override(mock(LibGfapi.class));
	}

	@AfterClass
	public static void tearDown()
	{
		LibGfapiProvider.clearOverride();
	}

	@Test
	public void openedFileIsTracked()
	{
		givenAConnectedSession();
		givenOpenSucceeds("aPath");

		GlusterFsFile file = session.open("aPath", 0);

		verify(resourceTracker).allocated(file);
	}

	@Test
	public void createdFileIsTracked()
	{
		givenAConnectedSession();
		givenCreateSucceeds("aPath");

		GlusterFsFile file = session.create("aPath", 0, 0);

		verify(resourceTracker).allocated(file);
	}

	private void givenCreateSucceeds(String aPath)
	{
		when(lib.glfs_creat(CONNECTED_SESSION_ID, aPath, (short) 0, Mode.valueOf(0))).thenReturn(OPENED_FILE_ID);
	}

	private void givenOpenSucceeds(String aPath)
	{
		when(lib.glfs_open(CONNECTED_SESSION_ID, aPath, (short) 0)).thenReturn(OPENED_FILE_ID);
	}

	private void givenAConnectedSession()
	{
		session.glFsPtr = CONNECTED_SESSION_ID;
	}

	@Test
	public void closeClosesResources()
	{
		givenAConnectedSession();

		session.close();

		verify(resourceTracker).closeResources();
		verify(logAccess).close(true);
		verify(lib).glfs_fini(CONNECTED_SESSION_ID);
	}

	@Test
	public void finalizeClosesWhenNotClosed() throws Throwable
	{
		givenAConnectedSession();

		session.finalize();

		verify(resourceTracker).closeResources();
		verify(logAccess).close(true);
		verify(lib).glfs_fini(CONNECTED_SESSION_ID);
	}

	@Test
	public void finalizeDoesntCloseWhenAlreadyClosed() throws Throwable
	{
		session.finalize();

		verify(resourceTracker, never()).closeResources();
		verify(logAccess, never()).close(true);
		verify(lib, never()).glfs_fini(anyLong());
	}
}