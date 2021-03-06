/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.cyanspring.avro.generate.trade.bean;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class CancelOrderReply extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"CancelOrderReply\",\"namespace\":\"com.cyanspring.avro.generate.trade.bean\",\"fields\":[{\"name\":\"objectType\",\"type\":\"int\"},{\"name\":\"orderId\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"exchangeAccount\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"result\",\"type\":\"boolean\"},{\"name\":\"message\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"txId\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public int objectType;
  @Deprecated public java.lang.String orderId;
  @Deprecated public java.lang.String exchangeAccount;
  @Deprecated public boolean result;
  @Deprecated public java.lang.String message;
  @Deprecated public java.lang.String txId;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public CancelOrderReply() {}

  /**
   * All-args constructor.
   */
  public CancelOrderReply(java.lang.Integer objectType, java.lang.String orderId, java.lang.String exchangeAccount, java.lang.Boolean result, java.lang.String message, java.lang.String txId) {
    this.objectType = objectType;
    this.orderId = orderId;
    this.exchangeAccount = exchangeAccount;
    this.result = result;
    this.message = message;
    this.txId = txId;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return objectType;
    case 1: return orderId;
    case 2: return exchangeAccount;
    case 3: return result;
    case 4: return message;
    case 5: return txId;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: objectType = (java.lang.Integer)value$; break;
    case 1: orderId = (java.lang.String)value$; break;
    case 2: exchangeAccount = (java.lang.String)value$; break;
    case 3: result = (java.lang.Boolean)value$; break;
    case 4: message = (java.lang.String)value$; break;
    case 5: txId = (java.lang.String)value$; break;
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
   * Gets the value of the 'orderId' field.
   */
  public java.lang.String getOrderId() {
    return orderId;
  }

  /**
   * Sets the value of the 'orderId' field.
   * @param value the value to set.
   */
  public void setOrderId(java.lang.String value) {
    this.orderId = value;
  }

  /**
   * Gets the value of the 'exchangeAccount' field.
   */
  public java.lang.String getExchangeAccount() {
    return exchangeAccount;
  }

  /**
   * Sets the value of the 'exchangeAccount' field.
   * @param value the value to set.
   */
  public void setExchangeAccount(java.lang.String value) {
    this.exchangeAccount = value;
  }

  /**
   * Gets the value of the 'result' field.
   */
  public java.lang.Boolean getResult() {
    return result;
  }

  /**
   * Sets the value of the 'result' field.
   * @param value the value to set.
   */
  public void setResult(java.lang.Boolean value) {
    this.result = value;
  }

  /**
   * Gets the value of the 'message' field.
   */
  public java.lang.String getMessage() {
    return message;
  }

  /**
   * Sets the value of the 'message' field.
   * @param value the value to set.
   */
  public void setMessage(java.lang.String value) {
    this.message = value;
  }

  /**
   * Gets the value of the 'txId' field.
   */
  public java.lang.String getTxId() {
    return txId;
  }

  /**
   * Sets the value of the 'txId' field.
   * @param value the value to set.
   */
  public void setTxId(java.lang.String value) {
    this.txId = value;
  }

  /** Creates a new CancelOrderReply RecordBuilder */
  public static com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder newBuilder() {
    return new com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder();
  }
  
  /** Creates a new CancelOrderReply RecordBuilder by copying an existing Builder */
  public static com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder newBuilder(com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder other) {
    return new com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder(other);
  }
  
  /** Creates a new CancelOrderReply RecordBuilder by copying an existing CancelOrderReply instance */
  public static com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder newBuilder(com.cyanspring.avro.generate.trade.bean.CancelOrderReply other) {
    return new com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder(other);
  }
  
  /**
   * RecordBuilder for CancelOrderReply instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<CancelOrderReply>
    implements org.apache.avro.data.RecordBuilder<CancelOrderReply> {

    private int objectType;
    private java.lang.String orderId;
    private java.lang.String exchangeAccount;
    private boolean result;
    private java.lang.String message;
    private java.lang.String txId;

    /** Creates a new Builder */
    private Builder() {
      super(com.cyanspring.avro.generate.trade.bean.CancelOrderReply.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.objectType)) {
        this.objectType = data().deepCopy(fields()[0].schema(), other.objectType);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.orderId)) {
        this.orderId = data().deepCopy(fields()[1].schema(), other.orderId);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.exchangeAccount)) {
        this.exchangeAccount = data().deepCopy(fields()[2].schema(), other.exchangeAccount);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.result)) {
        this.result = data().deepCopy(fields()[3].schema(), other.result);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.message)) {
        this.message = data().deepCopy(fields()[4].schema(), other.message);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.txId)) {
        this.txId = data().deepCopy(fields()[5].schema(), other.txId);
        fieldSetFlags()[5] = true;
      }
    }
    
    /** Creates a Builder by copying an existing CancelOrderReply instance */
    private Builder(com.cyanspring.avro.generate.trade.bean.CancelOrderReply other) {
            super(com.cyanspring.avro.generate.trade.bean.CancelOrderReply.SCHEMA$);
      if (isValidValue(fields()[0], other.objectType)) {
        this.objectType = data().deepCopy(fields()[0].schema(), other.objectType);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.orderId)) {
        this.orderId = data().deepCopy(fields()[1].schema(), other.orderId);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.exchangeAccount)) {
        this.exchangeAccount = data().deepCopy(fields()[2].schema(), other.exchangeAccount);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.result)) {
        this.result = data().deepCopy(fields()[3].schema(), other.result);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.message)) {
        this.message = data().deepCopy(fields()[4].schema(), other.message);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.txId)) {
        this.txId = data().deepCopy(fields()[5].schema(), other.txId);
        fieldSetFlags()[5] = true;
      }
    }

    /** Gets the value of the 'objectType' field */
    public java.lang.Integer getObjectType() {
      return objectType;
    }
    
    /** Sets the value of the 'objectType' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder setObjectType(int value) {
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
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder clearObjectType() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'orderId' field */
    public java.lang.String getOrderId() {
      return orderId;
    }
    
    /** Sets the value of the 'orderId' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder setOrderId(java.lang.String value) {
      validate(fields()[1], value);
      this.orderId = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'orderId' field has been set */
    public boolean hasOrderId() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'orderId' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder clearOrderId() {
      orderId = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'exchangeAccount' field */
    public java.lang.String getExchangeAccount() {
      return exchangeAccount;
    }
    
    /** Sets the value of the 'exchangeAccount' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder setExchangeAccount(java.lang.String value) {
      validate(fields()[2], value);
      this.exchangeAccount = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'exchangeAccount' field has been set */
    public boolean hasExchangeAccount() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'exchangeAccount' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder clearExchangeAccount() {
      exchangeAccount = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'result' field */
    public java.lang.Boolean getResult() {
      return result;
    }
    
    /** Sets the value of the 'result' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder setResult(boolean value) {
      validate(fields()[3], value);
      this.result = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'result' field has been set */
    public boolean hasResult() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'result' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder clearResult() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'message' field */
    public java.lang.String getMessage() {
      return message;
    }
    
    /** Sets the value of the 'message' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder setMessage(java.lang.String value) {
      validate(fields()[4], value);
      this.message = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'message' field has been set */
    public boolean hasMessage() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'message' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder clearMessage() {
      message = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'txId' field */
    public java.lang.String getTxId() {
      return txId;
    }
    
    /** Sets the value of the 'txId' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder setTxId(java.lang.String value) {
      validate(fields()[5], value);
      this.txId = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'txId' field has been set */
    public boolean hasTxId() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'txId' field */
    public com.cyanspring.avro.generate.trade.bean.CancelOrderReply.Builder clearTxId() {
      txId = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    @Override
    public CancelOrderReply build() {
      try {
        CancelOrderReply record = new CancelOrderReply();
        record.objectType = fieldSetFlags()[0] ? this.objectType : (java.lang.Integer) defaultValue(fields()[0]);
        record.orderId = fieldSetFlags()[1] ? this.orderId : (java.lang.String) defaultValue(fields()[1]);
        record.exchangeAccount = fieldSetFlags()[2] ? this.exchangeAccount : (java.lang.String) defaultValue(fields()[2]);
        record.result = fieldSetFlags()[3] ? this.result : (java.lang.Boolean) defaultValue(fields()[3]);
        record.message = fieldSetFlags()[4] ? this.message : (java.lang.String) defaultValue(fields()[4]);
        record.txId = fieldSetFlags()[5] ? this.txId : (java.lang.String) defaultValue(fields()[5]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
