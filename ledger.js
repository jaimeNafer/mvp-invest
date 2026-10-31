/* Lastro ledger: management accounting in BRL, not a tax rules engine. */
(function(root,factory){if(typeof module==='object'&&module.exports)module.exports=factory();else root.Ledger=factory()})(typeof globalThis!=='undefined'?globalThis:this,function(){
'use strict';
const EPS=1e-9,TYPES=['OPENING','BUY','SELL','CONVERT','DEPOSIT','WITHDRAW','TRANSFER','TAX'];
function finite(n){return typeof n==='number'&&Number.isFinite(n)}
function validate(e){if(!e||!e.id||!e.owner||!e.venue||!e.date||!TYPES.includes(e.type))throw Error('Identificação, titular, corretora, data e tipo são obrigatórios.');if(!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(e.date)||!Number.isFinite(Date.parse(e.date+':00-03:00')))throw Error('Data inválida.');if(new Date(e.date.slice(0,10)+'T00:00:00Z').toISOString().slice(0,10)!==e.date.slice(0,10))throw Error('Data de calendário inválida.');if(!/^[A-Z0-9]{2,12}$/.test(e.asset||''))throw Error('Símbolo do ativo inválido.');if(!finite(e.qty)||e.qty<=0||e.qty>1e12)throw Error('Quantidade precisa ser positiva e até 1 trilhão.');if(!finite(e.fx)||e.fx<=0||e.fx>1e9)throw Error('Informe a cotação em BRL da moeda da operação.');if(!['BR','EX','UNKNOWN'].includes(e.country))throw Error('Informe a localização da instituição.');if(['BUY','SELL','CONVERT'].includes(e.type)){if(!/^[A-Z0-9]{2,12}$/.test(e.quote||'')||e.quote===e.asset)throw Error('Moeda de negociação inválida ou igual ao ativo.');if(!finite(e.price)||e.price<=0||e.price>1e12)throw Error('Preço unitário inválido.');if(e.quote==='BRL'&&e.fx!==1)throw Error('Cotação BRL deve ser 1.');}if(['DEPOSIT','WITHDRAW','TAX'].includes(e.type)&&!['BRL','USD'].includes(e.asset))throw Error('Aporte, retirada e imposto usam BRL ou USD; para cripto use transferência ou saldo inicial.');if(e.asset==='BRL'&&['DEPOSIT','WITHDRAW','TAX'].includes(e.type)&&e.fx!==1)throw Error('Cotação BRL deve ser 1.');if(e.type==='OPENING'&&(!finite(e.costBRL)||e.costBRL<0||e.costBRL>1e18))throw Error('Custo histórico inicial inválido.');if(e.type==='TRANSFER'&&(!e.toVenue||e.toVenue===e.venue))throw Error('Informe um destino diferente da origem.');if(!finite(e.feeQty)||e.feeQty<0||e.feeQty>1e12)throw Error('Taxa inválida.');if(e.feeQty>0&&(!/^[A-Z0-9]{2,12}$/.test(e.feeCurrency||'')||!finite(e.feeFx)||e.feeFx<=0))throw Error('Informe moeda, quantidade e valor em BRL por unidade da taxa.');if(e.feeCurrency==='BRL'&&e.feeQty>0&&e.feeFx!==1)throw Error('Cotação da taxa em BRL deve ser 1.');if(['OPENING','DEPOSIT','WITHDRAW','TAX'].includes(e.type)&&e.feeQty)throw Error('Neste MVP, registre estes lançamentos sem taxa; custos de rede pertencem à transferência.');if(e.type==='SELL'&&e.feeCurrency===e.quote&&e.feeQty>=e.qty*e.price)throw Error('A taxa não pode consumir toda a venda.');if(e.type==='OPENING'&&e.asset==='BRL'&&Math.abs(e.costBRL-e.qty)>EPS)throw Error('O custo do saldo BRL deve ser igual ao saldo.');if(e.type==='BUY'&&e.feeCurrency===e.asset&&e.feeQty>=e.qty)throw Error('A taxa não pode consumir toda a compra.');if(e.type==='CONVERT'&&e.feeCurrency===e.asset&&e.feeQty>=e.qty)throw Error('A taxa não pode consumir toda a conversão.');return e}
function replay(events,options={}){
 const rows=[],accounts={},custody={},flows=[],taxes=[],initial=[],marks={};let owners=new Set(),lastDate='';
 const akey=(o,a)=>o+'|'+a,ckey=(o,v,a)=>o+'|'+v+'|'+a;
 const get=(o,a)=>accounts[akey(o,a)]||(accounts[akey(o,a)]={owner:o,asset:a,qty:0,cost:0});
 function credit(o,v,a,q,c){let x=get(o,a);x.qty+=q;x.cost+=c;custody[ckey(o,v,a)]=(custody[ckey(o,v,a)]||0)+q}
 function debit(o,v,a,q){let x=get(o,a),available=custody[ckey(o,v,a)]||0;if(q>available+EPS||q>x.qty+EPS)throw Error(`Saldo insuficiente de ${a} em ${v}: disponível ${available.toLocaleString('pt-BR',{maximumFractionDigits:8})}. Registre a compra, aporte ou saldo inicial anterior.`);let cost=x.qty>EPS?x.cost/x.qty*q:0;x.qty-=q;x.cost-=cost;custody[ckey(o,v,a)]=available-q;if(Math.abs(x.qty)<EPS){x.qty=0;x.cost=0}return cost}
 const sorted=[...events].sort((a,b)=>a.date.localeCompare(b.date)||(a.sequence||0)-(b.sequence||0)||String(a.id).localeCompare(String(b.id)));let seen=new Set();
 for(const e of sorted){validate(e);if(seen.has(e.id))throw Error('Identificador de lançamento duplicado.');seen.add(e.id);if(options.owner&&options.owner!=='all'&&e.owner!==options.owner)continue;if(options.end&&e.date.slice(0,10)>options.end)continue;owners.add(e.owner);lastDate=e.date;const fee=e.feeQty*e.feeFx||0,legs=[];let gross=0,net=0,cost=0,tradeResult=null,conversionResult=0,feeResult=0;
 function spend(a,q,value,category){if(q<=0)return 0;let c=debit(e.owner,e.venue,a,q);let result=a==='BRL'?0:value-c;legs.push({asset:a,qty:q,valueBRL:value,costBRL:c,resultBRL:result,category});if(category==='fee')feeResult+=result;else conversionResult+=result;return c}
 if(e.type==='OPENING'){credit(e.owner,e.venue,e.asset,e.qty,e.costBRL);initial.push({owner:e.owner,date:e.date,value:e.costBRL});marks[e.asset]=e.asset==='BRL'?1:e.costBRL/e.qty;}
 else if(e.type==='DEPOSIT'){credit(e.owner,e.venue,e.asset,e.qty,e.qty*e.fx);flows.push({owner:e.owner,date:e.date,value:e.qty*e.fx});marks[e.asset]=e.fx}
 else if(e.type==='WITHDRAW'){let c=debit(e.owner,e.venue,e.asset,e.qty),value=e.qty*e.fx;if(e.asset!=='BRL'){conversionResult=value-c;legs.push({asset:e.asset,qty:e.qty,valueBRL:value,costBRL:c,resultBRL:conversionResult,category:'withdraw-fx'})}flows.push({owner:e.owner,date:e.date,value:-value});marks[e.asset]=e.fx}
 else if(e.type==='TAX'){let value=e.qty*e.fx;if(e.taxSource==='portfolio'){spend(e.asset,e.qty,value,'tax-fx')}taxes.push({owner:e.owner,date:e.date,value,period:e.taxPeriod||'',reference:e.reference||'',source:e.taxSource||'external'});}
 else if(e.type==='TRANSFER'){const from=ckey(e.owner,e.venue,e.asset),to=ckey(e.owner,e.toVenue,e.asset);if((custody[from]||0)+EPS<e.qty)throw Error('Quantidade de transferência acima do saldo na origem.');custody[from]-=e.qty;custody[to]=(custody[to]||0)+e.qty;if(fee){let c=spend(e.feeCurrency,e.feeQty,fee,'fee');feeResult-=fee;legs.push({asset:e.feeCurrency,qty:0,valueBRL:-fee,costBRL:0,resultBRL:-fee,category:'network-expense'});net=-fee;} }
 else{
 gross=e.qty*e.price*e.fx;marks[e.asset]=e.price*e.fx;marks[e.quote]=e.fx;
 if(['BUY','CONVERT'].includes(e.type)){
  spend(e.quote,e.qty*e.price,gross,e.type==='CONVERT'?'conversion':'funding');
  let received=e.qty;if(e.feeCurrency===e.asset&&fee){received-=e.feeQty;cost=gross;}else{if(fee)spend(e.feeCurrency,e.feeQty,fee,'fee');cost=gross+fee;}
  credit(e.owner,e.venue,e.asset,received,cost);net=gross+((e.feeCurrency===e.asset)?0:fee);
 }else{
  const consumed=e.qty+(e.feeCurrency===e.asset?e.feeQty:0);cost=debit(e.owner,e.venue,e.asset,consumed);const quoteFee=e.feeCurrency===e.quote?e.feeQty:0;
  credit(e.owner,e.venue,e.quote,e.qty*e.price-quoteFee,(e.qty*e.price-quoteFee)*e.fx);
  if(e.feeQty&&e.feeCurrency!==e.quote&&e.feeCurrency!==e.asset)spend(e.feeCurrency,e.feeQty,fee,'fee');
  net=gross-(e.feeCurrency===e.asset?0:fee);tradeResult=net-cost;legs.push({asset:e.asset,qty:consumed,valueBRL:net,costBRL:cost,resultBRL:tradeResult,category:'sale'});
 }
 }
 const result=(tradeResult||0)+conversionResult+feeResult;
 rows.push({...e,grossBRL:gross,netBRL:net,costBasisBRL:cost,feeBRL:fee,tradeResult,conversionResult,feeResult,resultBRL:result,legs,inventoryAfter:get(e.owner,e.asset).qty,avgAfter:get(e.owner,e.asset).qty>EPS?get(e.owner,e.asset).cost/get(e.owner,e.asset).qty:0});
 }
 const holdings=Object.values(accounts).filter(a=>a.qty>EPS).map(a=>({...a,average:a.cost/a.qty}));
 return{rows,holdings,custody,flows,taxes,initial,marks,owners:[...owners],lastDate};
}
function period(report,start='',end='9999-12-31') {const inside=d=>d.slice(0,10)>=start&&d.slice(0,10)<=end,rows=report.rows.filter(e=>inside(e.date)),sales=rows.filter(e=>e.type==='SELL'),tax=report.taxes.filter(e=>inside(e.date)).reduce((s,e)=>s+e.value,0);return{rows,count:rows.filter(e=>['BUY','SELL','CONVERT'].includes(e.type)).length,sales:sales.length,wins:sales.filter(e=>e.tradeResult>EPS).length,losses:sales.filter(e=>e.tradeResult< -EPS).length,flat:sales.filter(e=>Math.abs(e.tradeResult)<=EPS).length,fees:rows.reduce((s,e)=>s+e.feeBRL,0),tax,realized:rows.reduce((s,e)=>s+e.resultBRL,0),saleResult:sales.reduce((s,e)=>s+e.tradeResult,0),conversion:rows.reduce((s,e)=>s+e.conversionResult+e.feeResult,0),volume:sales.reduce((s,e)=>s+e.grossBRL,0),deposits:report.flows.filter(e=>inside(e.date)&&e.value>0).reduce((s,e)=>s+e.value,0),withdrawals:report.flows.filter(e=>inside(e.date)&&e.value<0).reduce((s,e)=>s-e.value,0)}}
function value(report,marks){return report.holdings.map(h=>({...h,mark:marks[h.asset]??report.marks[h.asset]??null,value:(marks[h.asset]??report.marks[h.asset]??0)*h.qty,unrealized:((marks[h.asset]??report.marks[h.asset]??0)*h.qty)-h.cost}))}
function monthly(report,start='',end='9999-12-31'){let months=[...new Set(report.rows.filter(r=>r.date.slice(0,10)>=start&&r.date.slice(0,10)<=end).map(r=>r.date.slice(0,7)))];return months.map(month=>({month,...period(report,start>month+'-01'?start:month+'-01',end<month+'-31'?end:month+'-31')}))}
function compareBalance(events,check){
 if(!check||!check.owner||!check.venue||!/^[A-Z0-9]{2,12}$/.test(check.asset||'')||!/^\d{4}-\d{2}-\d{2}$/.test(check.date||'')||!Number.isFinite(Date.parse(check.date+'T00:00:00Z'))||new Date(check.date+'T00:00:00Z').toISOString().slice(0,10)!==check.date||!finite(check.qty)||check.qty<0||check.qty>1e12)throw Error('Informe titular, conta, ativo, data válida e saldo não negativo.');
 const r=replay(events,{owner:check.owner,end:check.date}),book=r.custody[check.owner+'|'+check.venue+'|'+check.asset]||0,difference=book-check.qty;
 return{...check,book,difference,matches:Math.abs(difference)<=EPS};
}
function findIssues(report){
 const issues=[],refs=new Map();
 for(const e of report.rows){
  if(e.country==='UNKNOWN')issues.push({id:e.id,kind:'country',label:'País da instituição ausente',help:'Informe Brasil ou exterior conforme os documentos da instituição.'});
  if(['BUY','SELL','CONVERT'].includes(e.type)&&!e.reference)issues.push({id:e.id,kind:'reference',label:'Falta ordem ou comprovante',help:'Adicione a referência para localizar esta operação no extrato.'});
  if(e.type==='OPENING'&&e.costBRL===0)issues.push({id:e.id,kind:'cost',label:'Custo inicial informado como zero',help:'Confira o custo de aquisição. Custo desconhecido não equivale a zero.'});
  if(e.reference){const key=JSON.stringify([e.owner,e.venue,e.reference]);if(!refs.has(key))refs.set(key,[]);refs.get(key).push(e);}
 }
 for(const entries of refs.values())if(entries.length>1)for(const e of entries)issues.push({id:e.id,kind:'repeated',label:'Referência repetida: confira',help:'Pode ser uma ordem com várias execuções ou um lançamento repetido. Compare os documentos antes de excluir.'});
 return issues;
}
function csv(rows){if(!rows.length)return'\ufeffSem registros\r\n';let keys=Object.keys(rows[0]);const cell=v=>{if(typeof v==='number')return String(v).replace('.',',');let s=String(v??'');if(/^[=+\-@\t\r]/.test(s))s="'"+s;return '"'+s.replace(/"/g,'""')+'"'};return '\ufeff'+[keys.map(cell).join(';'),...rows.map(r=>keys.map(k=>cell(r[k])).join(';'))].join('\r\n')}
return{validate,replay,period,value,monthly,compareBalance,findIssues,csv,EPS,TYPES};
});
