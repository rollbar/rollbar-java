package com.rollbar.notifier.transformer;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rollbar.api.payload.data.Data;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TransformerPipelineTest {

  @Rule
  public MockitoRule rule = new MockitoJUnit().rule();

  @Test
  public void shouldCallTransformerInOrder() {
    Transformer transformer1 = mock(Transformer.class);
    Transformer transformer2 = mock(Transformer.class);

    Data sourceData = mock(Data.class);
    Data output1 = mock(Data.class);
    Data output2 = mock(Data.class);

    when(transformer1.transform(sourceData)).thenReturn(output1);
    when(transformer2.transform(output1)).thenReturn(output2);

    TransformerPipeline sut = new TransformerPipeline(asList(transformer1, transformer2));

    Data result = sut.transform(sourceData);

    assertThat(result, is(output2));
    verify(transformer1).transform(sourceData);
    verify(transformer2).transform(output1);
  }

  @Test
  public void shouldNotThrowErrorIfPipelineIsNull() {
    TransformerPipeline sut = new TransformerPipeline(null);

    Data sourceData = mock(Data.class);

    Data result = sut.transform(sourceData);

    assertThat(result, is(sourceData));
  }

  @Test
  public void shouldNotTransformDataIfEmpty() {
    TransformerPipeline sut = new TransformerPipeline(new ArrayList<>());

    Data sourceData = mock(Data.class);

    Data result = sut.transform(sourceData);

    assertThat(result, is(sourceData));
  }
}