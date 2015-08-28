package com.cyanspring.common.filter;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cyanspring.common.staticdata.RefData;
import com.cyanspring.common.staticdata.fu.IType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INFO/spring/ContractDateFilterTest.xml" })
public class TestContractDateFilter {

	@Autowired
	@Qualifier("contractDateFilter")
	IRefDataFilter iDataFilter;
	
	RefData refData1;
	RefData refData2;
	RefData refData3;
	List<RefData> lstRefData;
	
	@Before
	public void before() {
		lstRefData = new ArrayList<RefData>();
	}
	
	@Test
	public void testRefDataFilter() throws Exception {
		// Valid settlement date, not expired nor more than 5-year
		refData1 = new RefData();
		refData1.setIType(IType.FUTURES_CX.getValue());
		refData1.setSymbol("IF1502");
		refData1.setCategory("AG");
		refData1.setExchange("SHF");
		refData1.setRefSymbol("AG12.SHF");
		refData1.setSettlementDate("2019-08-21");

		// This record has expired settlement date, will be filtered out
		refData2 = new RefData();
		refData2.setIType(IType.FUTURES.getValue());
		refData2.setSymbol("ag1511.SHF");
		refData2.setCategory("BG");
		refData2.setExchange("SHF");
		refData2.setSettlementDate("2014-08-21");

		// This record has more than 5-year settlement date, will be filtered out
		refData3 = new RefData();
		refData3.setIType(IType.FUTURES_CX.getValue());
		refData3.setSymbol("IF1502");
		refData3.setCategory("AG");
		refData3.setExchange("SHF");
		refData3.setRefSymbol("AG.SHF");
		refData3.setSettlementDate("2095-08-21");

		lstRefData.add(refData1);
		lstRefData.add(refData2);
		lstRefData.add(refData3);
		assertEquals(3, lstRefData.size());

		List<RefData> lstFilteredRefData = (List<RefData>) iDataFilter.filter(lstRefData);
		assertEquals(1, lstFilteredRefData.size());
	}
	
}