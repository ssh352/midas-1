/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.cyanspring.avro.generate.market.bean;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class UnsubscribeQuote extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UnsubscribeQuote\",\"namespace\":\"com.cyanspring.avro.generate.market.bean\",\"fields\":[{\"name\":\"objectType\",\"type\":\"int\"},{\"name\":\"symbols\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public int objectType;
  @Deprecated public java.util.List<java.lang.String> symbols;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public UnsubscribeQuote() {}

  /**
   * All-args constructor.
   */
  public UnsubscribeQuote(java.lang.Integer objectType, java.util.List<java.lang.String> symbols) {
    this.objectType = objectType;
    this.symbols = symbols;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return objectType;
    case 1: return symbols;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: objectType = (java.lang.Integer)value$; break;
    case 1: symbols = (java.util.List<java.lang.String>)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'objectType' field.
   */
  public java.lang.Integer getObjectType() {
    return objectType;
  }

  /**
   * Sets the value of the 'objectType' field.
   * @param value the value to set.
   */
  public void setObjectType(java.lang.Integer value) {
    this.objectType = value;
  }

  /**
   * Gets the value of the 'symbols' field.
   */
  public java.util.List<java.lang.String> getSymbols() {
    return symbols;
  }

  /**
   * Sets the value of the 'symbols' field.
   * @param value the value to set.
   */
  public void setSymbols(java.util.List<java.lang.String> value) {
    this.symbols = value;
  }

  /** Creates a new UnsubscribeQuote RecordBuilder */
  public static com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder newBuilder() {
    return new com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder();
  }
  
  /** Creates a new UnsubscribeQuote RecordBuilder by copying an existing Builder */
  public static com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder newBuilder(com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder other) {
    return new com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder(other);
  }
  
  /** Creates a new UnsubscribeQuote RecordBuilder by copying an existing UnsubscribeQuote instance */
  public static com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder newBuilder(com.cyanspring.avro.generate.market.bean.UnsubscribeQuote other) {
    return new com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder(other);
  }
  
  /**
   * RecordBuilder for UnsubscribeQuote instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<UnsubscribeQuote>
    implements org.apache.avro.data.RecordBuilder<UnsubscribeQuote> {

    private int objectType;
    private java.util.List<java.lang.String> symbols;

    /** Creates a new Builder */
    private Builder() {
      super(com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.objectType)) {
        this.objectType = data().deepCopy(fields()[0].schema(), other.objectType);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.symbols)) {
        this.symbols = data().deepCopy(fields()[1].schema(), other.symbols);
        fieldSetFlags()[1] = true;
      }
    }
    
    /** Creates a Builder by copying an existing UnsubscribeQuote instance */
    private Builder(com.cyanspring.avro.generate.market.bean.UnsubscribeQuote other) {
            super(com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.SCHEMA$);
      if (isValidValue(fields()[0], other.objectType)) {
        this.objectType = data().deepCopy(fields()[0].schema(), other.objectType);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.symbols)) {
        this.symbols = data().deepCopy(fields()[1].schema(), other.symbols);
        fieldSetFlags()[1] = true;
      }
    }

    /** Gets the value of the 'objectType' field */
    public java.lang.Integer getObjectType() {
      return objectType;
    }
    
    /** Sets the value of the 'objectType' field */
    public com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder setObjectType(int value) {
      validate(fields()[0], value);
      this.objectType = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'objectType' field has been set */
    public boolean hasObjectType() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'objectType' field */
    public com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder clearObjectType() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'symbols' field */
    public java.util.List<java.lang.String> getSymbols() {
      return symbols;
    }
    
    /** Sets the value of the 'symbols' field */
    public com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder setSymbols(java.util.List<java.lang.String> value) {
      validate(fields()[1], value);
      this.symbols = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'symbols' field has been set */
    public boolean hasSymbols() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'symbols' field */
    public com.cyanspring.avro.generate.market.bean.UnsubscribeQuote.Builder clearSymbols() {
      symbols = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    public UnsubscribeQuote build() {
      try {
        UnsubscribeQuote record = new UnsubscribeQuote();
        record.objectType = fieldSetFlags()[0] ? this.objectType : (java.lang.Integer) defaultValue(fields()[0]);
        record.symbols = fieldSetFlags()[1] ? this.symbols : (java.util.List<java.lang.String>) defaultValue(fields()[1]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
