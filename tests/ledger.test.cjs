const assert=require('node:assert/strict');
const fs=require('node:fs');
const E=require(fs.existsSync('ledger.js')?'../ledger.js':'../dist/ledger.js');
function event(id,type,asset,qty,extra={}){return{id:String(id),sequence:id,date:`2026-01-${String(id).padStart(2,'0')}T10:00`,owner:'A',venue:'X',country:'BR',type,asset,qty,fx:1,feeQty:0,feeFx:1,feeCurrency:'BRL',...extra}}
const initial=event(1,'DEPOSIT','BRL',10000);
let events=[initial,event(2,'BUY','BTC',2,{price:1000,quote:'BRL',feeQty:20}),event(3,'BUY','BTC',1,{price:1300,quote:'BRL',feeQty:10}),event(4,'SELL','BTC',1,{price:1500,quote:'BRL',feeQty:15})];
let r=E.replay(events),btc=r.holdings.find(x=>x.asset==='BTC');
assert.equal(btc.qty,2);assert.equal(btc.cost,2220);assert.equal(btc.average,1110);
assert.equal(r.rows[3].tradeResult,375);assert.equal(E.period(r).fees,45);
assert.equal(E.period(r).realized,375,'fees already included, not deducted twice');
assert.equal(E.period(r).wins,1);assert.equal(E.period(r).count,3);
let cut=E.period(r,'2026-01-04','2026-01-04');assert.equal(cut.realized,375,'cost basis must include all earlier buys');
assert.equal(E.monthly(r,'2026-01-04','2026-01-04')[0].count,1,'partial month must respect start filter');
assert.equal(E.replay([...events].reverse()).rows[3].tradeResult,375,'replay is chronological');
assert.throws(()=>E.replay(events.slice(1)),/Saldo insuficiente/);
assert.throws(()=>E.replay([...events,event(5,'SELL','BTC',3,{price:1000,quote:'BRL'})]),/Saldo insuficiente/);
assert.throws(()=>E.replay([...events,event(5,'SELL','BTC',1,{price:1000,quote:'BRL',venue:'Y'})]),/Saldo insuficiente/);
let owners=E.replay(events.concat(event(5,'OPENING','BTC',1,{owner:'B',costBRL:9999})));assert.equal(owners.holdings.find(x=>x.owner==='A'&&x.asset==='BTC').average,1110);assert.equal(owners.holdings.find(x=>x.owner==='B').average,9999);
let coins=[event(1,'OPENING','USDC',1000,{costBRL:5000}),event(2,'BUY','BTC',1,{price:100,quote:'USDC',fx:6,feeQty:1,feeCurrency:'USDC',feeFx:6}),event(3,'SELL','BTC',.5,{price:150,quote:'USDC',fx:6,feeQty:.5,feeCurrency:'USDC',feeFx:6})];
r=E.replay(coins);assert.equal(r.rows[1].conversionResult,100,'spending stablecoins releases their historical BRL cost');assert.equal(r.rows[1].feeResult,1);assert.equal(r.rows[1].resultBRL,101);assert.equal(r.holdings.find(x=>x.asset==='BTC').average,606);assert.equal(r.rows[2].tradeResult,144);assert.ok(Math.abs(r.holdings.find(x=>x.asset==='USDC').qty-973.5)<1e-8);
let convert=E.replay([initial,event(2,'BUY','USDC',100,{price:5,quote:'BRL'}),event(3,'CONVERT','BTC',.001,{price:100000,quote:'USDC',fx:6})]);assert.equal(convert.rows[2].conversionResult,100);assert.equal(convert.holdings.find(x=>x.asset==='BTC').cost,600);assert.ok(!convert.holdings.find(x=>x.asset==='USDC'));
r=E.replay([initial,event(2,'BUY','BTC',2,{price:100,quote:'BRL',feeQty:.1,feeCurrency:'BTC',feeFx:100})]);assert.equal(r.holdings.find(x=>x.asset==='BTC').qty,1.9);assert.equal(r.holdings.find(x=>x.asset==='BTC').cost,200);
r=E.replay([event(1,'OPENING','BTC',2,{costBRL:200}),event(2,'SELL','BTC',1,{price:150,quote:'BRL',feeQty:.1,feeCurrency:'BTC',feeFx:150})]);assert.ok(Math.abs(r.rows[1].tradeResult-40)<1e-8);assert.ok(Math.abs(r.holdings.find(x=>x.asset==='BTC').qty-.9)<1e-8);
r=E.replay([initial,event(2,'OPENING','BNB',2,{costBRL:100}),event(3,'BUY','BTC',1,{price:100,quote:'BRL',feeQty:.1,feeCurrency:'BNB',feeFx:60})]);assert.equal(r.holdings.find(x=>x.asset==='BTC').cost,106);assert.equal(r.rows[2].feeResult,1);
r=E.replay([event(1,'OPENING','BTC',2,{costBRL:200}),event(2,'TRANSFER','BTC',1,{toVenue:'Y',feeQty:.01,feeCurrency:'BTC',feeFx:150})]);assert.equal(r.holdings.find(x=>x.asset==='BTC').average,100);assert.ok(Math.abs(r.rows[1].resultBRL+1)<1e-8,'network fee reduces result by carrying cost after fee disposal');assert.equal(r.custody['A|Y|BTC'],1);
r=E.replay([initial,event(2,'WITHDRAW','BRL',1000),event(3,'TAX','BRL',100,{taxSource:'external',taxPeriod:'2025-12'}),event(4,'TAX','BRL',50,{taxSource:'portfolio'})]);assert.equal(r.holdings[0].qty,8950);assert.equal(E.period(r).realized,0);assert.equal(E.period(r).tax,150);assert.equal(E.period(r).withdrawals,1000);
assert.throws(()=>E.validate(event(1,'SELL','BTC',1,{quote:'USDC',price:100,feeQty:101,feeCurrency:'USDC',feeFx:5})),/consumir/);
assert.throws(()=>E.validate(event(1,'OPENING','BRL',100,{costBRL:200})),/custo/);
assert.throws(()=>E.validate({...initial,date:'2026-02-31T10:00'}),/calendário/);
assert.throws(()=>E.replay([initial,initial]),/duplicado/);
assert.ok(E.csv([{reference:'=HYPERLINK("x")',value:-2}]).includes("'=HYPERLINK"));assert.ok(E.csv([{name:'A;B',value:1.5}]).includes('1,5'));
console.log('PASS: cost basis, historical FX, cash, custody, conversions, fees, owners, date filters, tax records, validation, CSV escaping');
const vm=require('node:vm');
const appSource=fs.readFileSync(fs.existsSync('app.js')?'app.js':'dist/app.js','utf8').split('let state=cl(seed)')[0];
vm.runInNewContext(appSource+`
 const report=Ledger.replay(seed.events);
 const reference=report.marks;
 const equity=Ledger.value(report,reference).reduce((s,h)=>s+h.value,0);
 const unrealized=Ledger.value(report,reference).reduce((s,h)=>s+h.unrealized,0);
 const capital=report.initial.reduce((s,x)=>s+x.value,0)+report.flows.reduce((s,x)=>s+x.value,0);
 const realized=Ledger.period(report).realized;
 assertions.ok(Math.abs(equity-capital-realized-unrealized)<1e-6,'demo must reconcile capital + realized + open result');
 assertions.equal(Ledger.period(report).wins,1);
 assertions.equal(Ledger.period(report).losses,1);
 for(const row of report.rows) assertions.ok(Math.abs(row.resultBRL-row.legs.reduce((s,l)=>s+l.resultBRL,0))<1e-6,'each row result must reconcile with its detailed legs');
`,{Ledger:E,assertions:assert});
console.log('PASS: application demo reconciles equity and every realized leg');

// External checks never change the book and always replay the selected date.
const compareEvents=[event(1,'DEPOSIT','BRL',5000),event(2,'BUY','BTC',2,{price:1000,quote:'BRL'}),event(3,'TRANSFER','BTC',.5,{toVenue:'Y'}),event(4,'OPENING','BTC',9,{owner:'B',costBRL:9000})];
const original=JSON.stringify(compareEvents);
assert.equal(E.compareBalance(compareEvents,{owner:'A',venue:'X',asset:'BTC',date:'2026-01-02',qty:2}).matches,true);
assert.equal(E.compareBalance(compareEvents,{owner:'A',venue:'X',asset:'BTC',date:'2026-01-03',qty:2}).difference,-.5);
assert.equal(E.compareBalance(compareEvents,{owner:'A',venue:'Y',asset:'BTC',date:'2026-01-04',qty:.5}).matches,true);
assert.equal(E.compareBalance(compareEvents,{owner:'B',venue:'X',asset:'BTC',date:'2026-01-04',qty:9}).matches,true);
assert.equal(E.compareBalance(compareEvents,{owner:'A',venue:'Z',asset:'BTC',date:'2026-01-04',qty:0}).matches,true);
assert.throws(()=>E.compareBalance(compareEvents,{owner:'A',venue:'X',asset:'BTC',date:'2026-02-30',qty:0}),/data válida/);
assert.throws(()=>E.compareBalance(compareEvents,{owner:'A',venue:'X',asset:'BTC',date:'2026-01-04',qty:-1}),/saldo não negativo/);
assert.equal(JSON.stringify(compareEvents),original,'checking does not adjust transactions');
const amended=compareEvents.map(e=>e.type==='TRANSFER'?{...e,qty:.75}:e);
assert.equal(E.compareBalance(amended,{owner:'A',venue:'Y',asset:'BTC',date:'2026-01-04',qty:.5}).matches,false,'historical correction invalidates a former match');
const withIssues=E.replay([event(1,'OPENING','BRL',5000,{costBRL:5000,country:'UNKNOWN'}),event(2,'BUY','BTC',1,{price:1000,quote:'BRL',reference:'ORDER'}),event(3,'BUY','BTC',1,{price:1000,quote:'BRL',reference:'ORDER'}),event(4,'OPENING','ETH',1,{costBRL:0})]);
assert.equal(E.findIssues(withIssues).filter(i=>i.kind==='repeated').length,2);
assert.equal(E.findIssues(withIssues).filter(i=>i.kind==='cost').length,1);
assert.equal(E.findIssues(withIssues).filter(i=>i.kind==='country').length,1);
console.log('PASS: external balance checks, dates, custody, owners, zero balances, historical changes and review flags');
// Annual support uses acquisition costs and all history through December 31.
const annualEvents=[{...event(1,'OPENING','BTC',2,{costBRL:2000}),date:'2024-12-15T10:00'},
 {...event(2,'SELL','BTC',.5,{price:1600,quote:'BRL'}),date:'2025-02-01T10:00'},
 {...event(3,'BUY','BTC',.25,{price:2000,quote:'BRL'}),date:'2025-11-01T10:00'},
 {...event(4,'TRANSFER','BTC',.25,{toVenue:'Y'}),date:'2025-12-20T10:00'},
 {...event(5,'OPENING','BTC',1,{owner:'B',costBRL:3000}),date:'2025-03-01T10:00'}];
const yearRows=E.annualHoldings(annualEvents,2025),aYear=yearRows.find(h=>h.owner==='A'&&h.asset==='BTC'),bYear=yearRows.find(h=>h.owner==='B'&&h.asset==='BTC');
assert.equal(aYear.previousQty,2);assert.equal(aYear.previousCost,2000);
assert.equal(aYear.qty,1.75);assert.equal(aYear.cost,2000,'closing is at cost, not latest price');
assert.equal(aYear.accounts.find(x=>x.venue==='Y').qty,.25);
assert.equal(bYear.hasPrevious,false,'missing prior history must not claim a confirmed zero');assert.equal(bYear.cost,3000);
assert.equal(E.annualHoldings(annualEvents,2025,'A').some(h=>h.owner==='B'),false);
const exited=E.annualHoldings([annualEvents[0],{...event(2,'SELL','BTC',2,{price:1500,quote:'BRL'}),date:'2025-12-31T10:00'}],2025,'A').find(h=>h.asset==='BTC');assert.equal(exited.qty,0);assert.equal(exited.cost,0);assert.equal(exited.previousCost,2000);
assert.throws(()=>E.annualHoldings([],NaN),/Ano inválido/);
console.log('PASS: annual acquisition cost, prior-year history, fully sold assets, late-year entries, custody and owners');

// Exercise actual export functions with a download sink, without browser/network.
const uiSource=fs.readFileSync(fs.existsSync('app.js')?'app.js':'dist/app.js','utf8').split("$('backupFile').onchange")[0];
vm.runInNewContext(uiSource+`
 let captured;
 download=(name,data,mime)=>{captured={name,data,mime};};
 owner='Eliane (demo)';start='2026-09-01';end='2026-09-30';
 exportAnnual();
 assertions.ok(Math.abs(Number(captured.data.split('\\r\\n').find(line=>line.includes('\"BTC\"')).split(';')[9].replace(',','.'))-7657.65)<1e-6);
 assertions.ok(!captured.data.includes('Jaime (demo)'),'annual export respects titular');
 exportFiscal();
 assertions.ok(captured.data.includes('custo_vendido_BRL'));
 assertions.ok(!captured.data.includes('Jaime (demo)'),'monthly export respects titular');
 assertions.equal(fiscalMonthly(report()).length,1,'partial month export respects date filter');
 exportFiscalReport();
 assertions.ok(captured.data.startsWith('<!doctype html>'));
 assertions.ok(captured.data.includes('Saldos anuais a custo de aquisição'));
 assertions.ok(captured.data.includes('Operações do período'));
 assertions.ok(!captured.data.includes('<button'),'portable report contains no inactive buttons or help icons');
 assertions.ok(!captured.data.includes('Jaime (demo)'),'report respects titular');
 assertions.ok(captured.data.includes('150,15'),'report includes actual historical result');
`,{Ledger:E,assertions:assert,document:{getElementById:()=>({})},localStorage:{getItem:()=>null},console});
console.log('PASS: actual monthly CSV, annual CSV and portable report exports with date/titular filters');
