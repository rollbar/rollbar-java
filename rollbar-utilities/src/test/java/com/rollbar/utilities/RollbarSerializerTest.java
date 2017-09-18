package com.rollbar.utilities;

import org.junit.Test;

import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;

public class RollbarSerializerTest {
    @Test
    public void testSerializeObject() throws Exception {
        RollbarSerializer s = new RollbarSerializer();
        Map<String, Object> obj = new HashMap<String, Object>();
        obj.put("a", "a \" b");
        obj.put("other", "some \\\" string");
        FakeData d = new FakeData(obj);
        String result = s.serialize(d);
        String expected1 = "{\"a\":\"a \\\" b\",\"other\":\"some \\\\\\\" string\"}";
        String expected2 = "{\"other\":\"some \\\\\\\" string\",\"a\":\"a \\\" b\"}";
        Assert.assertTrue(expected1.equals(result) || expected2.equals(result));
    }

    @Test
    public void testSerializeArray() throws Exception {
        RollbarSerializer s = new RollbarSerializer();
        Map<String, Object> obj = new HashMap<String, Object>();
        obj.put("k", "v");
        Map<String, Object> innerObj = new HashMap<String, Object>();
        innerObj.put("x", "y");
        obj.put("inner", innerObj);
        Object[] a = {42,obj,"hello"};
        FakeData d = new FakeData(a);
        String result = s.serialize(d);
        String expected1 = "[42,{\"k\":\"v\",\"inner\":{\"x\":\"y\"}},\"hello\"]";
        String expected2 = "[42,{\"inner\":{\"x\":\"y\"},\"k\":\"v\"},\"hello\"]";
        Assert.assertTrue(expected1.equals(result) || expected2.equals(result));
    }

    @Test
    public void testSerializePretty() throws Exception {
        RollbarSerializer s = new RollbarSerializer(true);
        Map<String, Object> obj = new HashMap<String, Object>();
        obj.put("key1", 42);
        FakeData d = new FakeData(obj);
        String result = s.serialize(d);
        Assert.assertEquals(
            "should serialize object properly",
            "{\n" +
            "  \"key1\": 42\n" +
            "}",
            result);
    }

    @Test
    public void testSerializeNull() throws Exception {
        RollbarSerializer s = new RollbarSerializer();
        Map<String, Object> obj = new HashMap<String, Object>();
        obj.put("key1", null);
        FakeData d = new FakeData(obj);
        String result = s.serialize(d);
        Assert.assertEquals(
            "should serialize object properly",
            "{\"key1\":null}",
            result);
    }
}

class FakeData implements JsonSerializable {
  private Object object;

  public FakeData(Object object) {
    this.object = object;
  }

  public Object asJson() {
    return this.object;
  }
}
