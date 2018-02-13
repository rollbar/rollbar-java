package com.rollbar.notifier.filter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Level;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class FilterPipelineTest {

  @Rule
  public MockitoRule rule = new MockitoJUnit().rule();

  @Test
  public void shouldCallFiltersInOrderForPreProcess() {
    Filter filter1 = mock(Filter.class);
    Filter filter2 = mock(Filter.class);

    when(filter1.preProcess(any(), any(), any(), any())).thenReturn(false);
    when(filter2.preProcess(any(), any(), any(), any())).thenReturn(true);

    Level level = Level.ERROR;
    Throwable error = new RuntimeException("Something went wrong.");
    Map<String, Object> custom = emptyMap();
    String description = "Error description";

    FilterPipeline sut = new FilterPipeline(asList(filter1, filter2));

    boolean result = sut.preProcess(level, error, custom, description);

    assertThat(result, is(true));
    verify(filter1).preProcess(level, error, custom, description);
    verify(filter2).preProcess(level, error, custom, description);
  }

  @Test
  public void shouldStopProcessingPreProcessIfTrueFound() {
    Filter filter1 = mock(Filter.class);
    Filter filter2 = mock(Filter.class);

    when(filter1.preProcess(any(), any(), any(), any())).thenReturn(true);
    when(filter2.preProcess(any(), any(), any(), any())).thenReturn(false);

    Level level = Level.ERROR;
    Throwable error = new RuntimeException("Something went wrong.");
    Map<String, Object> custom = emptyMap();
    String description = "Error description";

    FilterPipeline sut = new FilterPipeline(asList(filter1, filter2));

    boolean result = sut.preProcess(level, error, custom, description);

    assertThat(result, is(true));
    verify(filter1).preProcess(level, error, custom, description);
    verify(filter2, never()).preProcess(level, error, custom, description);
  }

  @Test
  public void shouldCallFiltersInOrderForPostProcess() {
    Filter filter1 = mock(Filter.class);
    Filter filter2 = mock(Filter.class);

    Data data = mock(Data.class);

    when(filter1.postProcess(data)).thenReturn(false);
    when(filter2.postProcess(data)).thenReturn(true);

    FilterPipeline sut = new FilterPipeline(asList(filter1, filter2));

    boolean result = sut.postProcess(data);

    assertThat(result, is(true));
    verify(filter1).postProcess(data);
    verify(filter2).postProcess(data);
  }

  @Test
  public void shouldStopProcessingPostProcessIfTrueFound() {
    Filter filter1 = mock(Filter.class);
    Filter filter2 = mock(Filter.class);

    Data data = mock(Data.class);

    when(filter1.postProcess(data)).thenReturn(true);
    when(filter2.postProcess(data)).thenReturn(false);

    FilterPipeline sut = new FilterPipeline(asList(filter1, filter2));

    boolean result = sut.postProcess(data);

    assertThat(result, is(true));
    verify(filter1).postProcess(data);
    verify(filter2, never()).postProcess(data);
  }

  @Test
  public void shouldNotThrowErrorIfPipelineIsNull() {
    FilterPipeline sut = new FilterPipeline(null);

    Data data = mock(Data.class);

    boolean resultPreProcess = sut.preProcess(Level.ERROR, new RuntimeException(),
        emptyMap(), "");
    boolean resultPostProcess = sut.postProcess(data);

    assertThat(resultPreProcess, is(false));
    assertThat(resultPostProcess, is(false));
  }

  @Test
  public void shouldNotFilterDataIfEmpty() {
    FilterPipeline sut = new FilterPipeline(new ArrayList<>());

    Data data = mock(Data.class);

    boolean resultPreProcess = sut.preProcess(Level.ERROR, new RuntimeException(),
        emptyMap(), "");
    boolean resultPostProcess = sut.postProcess(data);

    assertThat(resultPreProcess, is(false));
    assertThat(resultPostProcess, is(false));
  }
}
