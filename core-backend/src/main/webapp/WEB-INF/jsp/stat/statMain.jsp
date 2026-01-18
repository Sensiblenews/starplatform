<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/taglibs.jsp"%>
<%@ page import="com.sensible.common.Constants"%>


<script type='text/javascript'>
$(function()
{
	//조회 클릭으로 검색
    $('#btn_search').click(function() {    	
		
		
		$('#memForm').attr('action','${ctx}/adm/memberList');		
		$('#memForm').submit();		
		return false;     
    	
    });
	
    $('#searchKeyword').keydown(function(event){
		if(event.keyCode == 13){
						
			$('#memForm').attr('action','${ctx}/adm/memberList');
			$('#memForm').submit();    
			return false;     
		}
	 	
	});
    
    
    $(".btn_detail").click(function(){
		var memId = $(this).attr("paramId");	
		$("#MEM_ID").val(memId);
		var option = {
				   url : '${ctx}/adm/memberDetail.do',
					//data : {'MEM_ID':memId},
		            target : '#openDialog',
		            dataType : 'html',   	            
		            success : function(data){
		            	var $o = $('#openDialog');
		            	$o.html(data);        	              
		             
		            }                   
		        };
		<c:if test="${_AUTH  eq 'SM'}">		
			$('#memForm').ajaxSubmit(option);   
		</c:if>
		return false;
	});
	
        
});

</script>


<div class="row wrapper border-bottom white-bg page-heading">
    <div class="col-lg-10">
        <h2>통계 현황</h2>       
    </div>
</div>

<div class="wrapper wrapper-content  animated fadeInUp">
	
	<div class="row">
	
		 	<%-- <div class="col-md-2">
                <div class="ibox float-e-margins">
                    
                    <div class="ibox-title">
                        <span class="label label-info pull-right">Member</span>
                        <h5>회원</h5>
                    </div>
                    <div class="ibox-content">
       
                         <h2 class="no-margins">${memberStat.CNT }</h2>                        
                    </div>
                </div>
            </div> --%>

        
            <div class="col-md-5">
                <div class="ibox float-e-margins">
                    <div class="ibox-title">
                        <span class="label label-info pull-right">Star</span>
                        <h5>별</h5>
     
                    </div>
                    <div class="ibox-content">

                        <div class="row">
                            <div class="col-md-4">
          
                               <h2 class="no-margins">☆ ${startStat.CON_PRICE}</h2>
                            </div>
                            <div class="col-md-4">
                   
                             <h2 class="no-margins">★ ${startStat.CON_PRICE_CHG}</h2>
                            </div>
                            <div class="col-md-4">
                            
	<h2 id="starSum" class="no-margins"></h2>
                            </div>
                        </div>
						<div class="row">
							<div class="col-md-4">
								<h3 id="emptyStar"></h3>
							</div>
							<div class="col-md-4">
								<h3 id="filledStar"></h3>
							</div>
							<div class="col-md-4">
								<h3 id="starSumPrice"></h3>
							</div>
						</div>
                    </div>
                </div>
   
             </div>
            
            <div class="col-md-5">
                <div class="ibox float-e-margins">
                    <div class="ibox-title">
                        <span class="label label-info pull-right">Heart</span>
                        <h5>하트</h5>
                    </div>
                    <div class="ibox-content">

                        <div class="row">
          
                           <div class="col-md-4">
                                <h2 class="no-margins">♡ ${heartStat.CON_HEART}</h2>
                            </div>
                   
                         <div class="col-md-4">
                                <h2 class="no-margins">♥ ${heartStat.CON_HEART_CHG}</h2>
                            </div>
                            
<div class="col-md-4">
                            	<h2 id="heartSum" class="no-margins"></h2>
                            </div>
                        </div>
                 
                       <div class="row">
							<div class="col-md-4">
								<h3 id="emptyHeart"></h3>
							</div>
							<div class="col-md-4">
								<h3 id="filledHeart"></h3>
							</div>
							<div class="col-md-4">
								<h3 id="heartSumPrice"></h3>
							</div>
						</div>

                    </div>
                </div>
            </div>
            <div class="col-md-2">
            	<div class="ibox float-e-margins">
           
             		<div class="ibox-title">
            			<span class="label label-info pull-right">Total</span>
                        <h5>총합</h5>
            		</div>
            		<div class="ibox-content">
	            		<div class="row">
	            			<div class="col-md-12">
	        
            				<h2 id="totalSum" class="no-margins"></h2>
	            			</div>
	            		</div>
	            		<div class="row">
	            			<div class="col-md-12">
	            				<h3 id="totalSumPrice"></h3>
	            			</div>
	            		</div>
	       
            	</div>
            	</div>
            </div>
	</div>
	
	<div class="row">
		<div class="ibox float-e-margins" style="width: 100%;">
			<div class="ibox-title">
				
                <div style="display: flex; gap: 20px; align-items: center;">
                	<h5 style="margin-right: 40px;">일별 통계 (갯수)</h5>
					<label><input type="radio" name="option1" value="empty_star" checked> 투명별</label>
					<label><input type="radio" name="option1" value="filled_star"> 진한별</label>
					<label><input type="radio" name="option1" value="empty_heart"> 투명하트</label>
					<label><input type="radio" name="option1" value="filled_heart"> 진한하트</label>
					<label><input type="radio" name="option1" value="total_amount"> 총액</label>
				</div>
			</div>
			<div class="ibox-content" style="height: 500px;">
				<canvas id="dailyChart"></canvas>
			</div>
		</div>
	</div>
	
	<div class="row">
		<div class="ibox float-e-margins" style="width: 100%;">
			<div class="ibox-title">
                <div style="display: flex; gap: 20px; align-items: center;">
               		<h5 style="margin-right: 40px;">월별 통계 (갯수)</h5>
					<label><input type="radio" name="option2" value="empty_star" checked> 투명별</label>
					<label><input type="radio" name="option2" value="filled_star"> 진한별</label>
					<label><input type="radio" name="option2" value="empty_heart"> 투명하트</label>
					<label><input type="radio" name="option2" value="filled_heart"> 진한하트</label>
					<label><input type="radio" name="option2" value="total_amount"> 총액</label>
				</div>
			</div>
			<div class="ibox-content" style="height: 500px;">
				<canvas id="monthlyChart"></canvas>
			</div>
		</div>
	</div>
</div>

<style>
	h3 {
		margin-left: 28px;
	}
</style>

<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>

<script>
  const graphDaily = JSON.parse('${graphDaily}');
  const graphMonthly = JSON.parse('${graphMonthly}');
  
  function getSpecificData(list, label) {
	  return list.map(d => d[label]);
  }
  
  // 금액 계산을 위한 헬퍼 함수
  function calculateTotalAmount(d) {
      const emptyStarAmount = (d.empty_star || 0) * 0.1;
      const filledStarAmount = (d.filled_star || 0) * 0.5;
      const emptyHeartAmount = (d.empty_heart || 0) * 0.1;
      const filledHeartAmount = (d.filled_heart || 0) * 0.5;
      return emptyStarAmount + filledStarAmount + emptyHeartAmount + filledHeartAmount;
  }
  
  function getBarLabel(type) {
	  switch (type) {
	  case 'empty_star': return '투명별';
	  case 'filled_star': return '진한별';
	  case 'empty_heart': return '투명하트';
	  case 'filled_heart': return '진한하트';
	  }
  }
  
  // 수정: drawChart 함수에 isCurrency 파라미터 추가
  function drawChart(ctx, labels, data, barLabel, isCurrency = false) {
	  const options = {
    	  responsive: true,
    	  maintainAspectRatio: false,
          scales: {
              y: { 
				  beginAtZero: true
			  }
          },
		  plugins: {
			  tooltip: {
				  callbacks: {}
			  }
		  }
      };
	  
	  // isCurrency 값에 따라 y축과 툴팁 포맷을 동적으로 변경
	  if (isCurrency) {
		  options.scales.y.ticks = {
			  callback: function(value) {
				  return value.toLocaleString('ko-KR') + '₩';
			  }
		  };
		  options.plugins.tooltip.callbacks.label = function(context) {
			  let label = context.dataset.label || '';
			  if (label) {
				  label += ': ';
			  }
			  if (context.parsed.y !== null) {
				  label += context.parsed.y.toLocaleString('ko-KR') + '₩';
			  }
			  return label;
		  };
	  }

	  return new Chart(ctx, {
          type: 'bar',
          data: {
              labels,
              datasets: [{
            	  label: barLabel,
                  data,
                  borderWidth: 1,
                  backgroundColor: 'rgba(75, 192, 192, 0.2)',
                  borderColor: 'rgba(75, 192, 192, 1)'
              }]
          },
          options: options
      });
  }

  // 상단 요약 정보 계산
  $(function() {
    const transparentStar = parseInt("${startStat.CON_PRICE}") * 0.1;
    const filledStar = parseInt("${startStat.CON_PRICE_CHG}") * 0.5;
    const transparentHeart = parseInt("${heartStat.CON_HEART}") * 0.1;
    const filledHeart = parseInt("${heartStat.CON_HEART_CHG}") * 0.5;
    const starSum = parseInt("${startStat.CON_PRICE}") + parseInt("${startStat.CON_PRICE_CHG}");
    const starSumPrice = transparentStar + filledStar;
    const heartSum = parseInt("${heartStat.CON_HEART}") + parseInt("${heartStat.CON_HEART_CHG}");
    const heartSumPrice = transparentHeart + filledHeart;

    $("#emptyStar").text(transparentStar.toLocaleString() + "₩");
    $("#filledStar").text(filledStar.toLocaleString() + "₩");
    $("#emptyHeart").text(transparentHeart.toLocaleString() + "₩");
    $("#filledHeart").text(filledHeart.toLocaleString() + "₩");
 
    $("#starSum").text("합 " + starSum.toString());
    $("#starSumPrice").text(starSumPrice.toLocaleString() + "₩");
    $("#heartSum").text("합 " + heartSum.toString());
    $("#heartSumPrice").text(heartSumPrice.toLocaleString() + "₩");
    $("#totalSum").text("총 " + (starSum + heartSum).toString());
    $("#totalSumPrice").text((starSumPrice + heartSumPrice).toLocaleString() + "₩");
  });
  
  // 일별 통계 차트
  $(function() {
	const labels = getSpecificData(graphDaily, "usage_date");
	let chartInstance = null;
	
	const _data = getSpecificData(graphDaily, "empty_star");
  	const _ctx = document.getElementById("dailyChart").getContext("2d");
  	chartInstance = drawChart(_ctx, labels, _data, getBarLabel('empty_star'), false);
	
	$("input[name='option1']").change(function() {
	  const selected = $("input[name='option1']:checked").val();
	  const ctx = document.getElementById("dailyChart").getContext("2d");
	  
	  let data, barLabel, isCurrency;
	  
	  // 수정: 선택 값에 따라 데이터와 옵션을 분기
	  if (selected === 'total_amount') {
		  data = graphDaily.map(d => calculateTotalAmount(d));
		  barLabel = '총액 (₩)';
		  isCurrency = true;
	  } else {
		  data = getSpecificData(graphDaily, selected);
		  barLabel = getBarLabel(selected);
		  isCurrency = false;
	  }
	  
	  if (chartInstance) {
		chartInstance.destroy();
	  }
	  
	  chartInstance = drawChart(ctx, labels, data, barLabel, isCurrency);
	});
  });
  
  // 월별 통계 차트
  $(function() {
	const labels = getSpecificData(graphMonthly, "usage_date");
	let chartInstance = null;
	
	const _data = getSpecificData(graphMonthly, "empty_star");
  	const _ctx = document.getElementById("monthlyChart").getContext("2d");
  	chartInstance = drawChart(_ctx, labels, _data, getBarLabel('empty_star'), false);
	
	$("input[name='option2']").change(function() {
	  const selected = $("input[name='option2']:checked").val();
	  const ctx = document.getElementById("monthlyChart").getContext("2d");
	  
	  let data, barLabel, isCurrency;

	  // 수정: 선택 값에 따라 데이터와 옵션을 분기
	  if (selected === 'total_amount') {
		  data = graphMonthly.map(d => calculateTotalAmount(d));
		  barLabel = '총액 (₩)';
		  isCurrency = true;
	  } else {
		  data = getSpecificData(graphMonthly, selected);
		  barLabel = getBarLabel(selected);
		  isCurrency = false;
	  }
	  
	  if (chartInstance) {
		chartInstance.destroy();
	  }
	  
	  chartInstance = drawChart(ctx, labels, data, barLabel, isCurrency);
	});
  });
</script>