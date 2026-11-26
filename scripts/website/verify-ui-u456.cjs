// Invoked by verify-l67.cjs --ui-u456 using the same isolated fixture and Chrome session.
module.exports=async function({evaluate,visit,viewport,check,shot,send,wait}){
 const pages=[];
 const routes=['/khoa-hoc','/chuong-trinh','/chi-nhanh','/giao-vien','/khoa-hoc/giao-tiep-a1','/chuong-trinh/tieng-anh','/chi-nhanh/chi-nhanh-1','/giao-vien/giao-vien-an','/tu-van','/gioi-thieu','/chinh-sach-bao-mat'];
 for(const width of [320,390,768,1024,1280,1366,1440]){
  await viewport(width, width===1366?768:1000);
  for(const route of routes){
   await visit(route);await evaluate('document.fonts.ready.then(()=>true)');
   await check('U4-U6 '+route+' has no overflow at '+width,'document.documentElement.scrollWidth<=innerWidth');
   await check('U4-U6 '+route+' heading fits reading scale at '+width,'parseFloat(getComputedStyle(document.querySelector("h1")).fontSize)<=44');
   pages.push(await evaluate(`({route:location.pathname,width:innerWidth,h1:getComputedStyle(document.querySelector('h1')).fontSize,firstCardTop:document.querySelector('.courseCard')?.getBoundingClientRect().top,formTop:document.querySelector('.consultationForm')?.getBoundingClientRect().top})`));
   if([390,1440].includes(width))await shot('u456-'+route.slice(1).replaceAll('/','-')+'-'+width);
  }
 }
 await viewport(1440,768);await visit('/khoa-hoc');
 await check('Catalog first row visible on laptop','document.querySelector(".courseCard").getBoundingClientRect().top<650');
 await check('Desktop advanced filters are visible','document.querySelector(".advancedFilters").open&&document.querySelector("select[name=audience]").getBoundingClientRect().height>=44');
 await evaluate('document.querySelector(".courseCard h2 a").textContent="Khóa tiếng Anh giao tiếp và phát triển kỹ năng dành cho người học có mục tiêu dài hạn"');
 await check('Long titles preserve aligned tuition rows',`(()=>{const cards=Array.from(document.querySelectorAll('.courseCard'));const a=cards[0].querySelector('.courseFacts').getBoundingClientRect(),b=cards[1].querySelector('.courseFacts').getBoundingClientRect();return Math.abs(a.bottom-b.bottom)<=1&&document.documentElement.scrollWidth<=innerWidth;})()`);
 await shot('u456-long-title-1440');
 await viewport(390,844);await visit('/khoa-hoc');
 await wait('!document.querySelector(".advancedFilters").open');
 await check('Unused mobile advanced filters collapse','!document.querySelector(".advancedFilters").open');
 await evaluate('document.querySelector(".advancedFilters summary").click()');await wait('document.querySelector(".advancedFilters").open');
 await check('Mobile filters expand to full-width readable controls','document.querySelector("select[name=branch]").getBoundingClientRect().width>250&&getComputedStyle(document.querySelector("select[name=branch]")).fontSize==="16px"');
 await shot('u456-mobile-filters-expanded');
 await visit('/khoa-hoc?audience=SECONDARY');await check('Active advanced filters stay expanded','document.querySelector(".advancedFilters").open&&document.querySelector("select[name=audience]").value==="SECONDARY"');
 await visit('/khoa-hoc/giao-tiep-a1?branch=chi-nhanh-1&audience=SECONDARY&page=1');
 await check('Class pagination preserves branch and audience',`(()=>{const a=document.querySelector('.pagination a'),u=new URL(a.href);return u.searchParams.get('branch')==='chi-nhanh-1'&&u.searchParams.get('audience')==='SECONDARY'&&u.searchParams.get('page')==='0';})()`);
 await evaluate('document.querySelector("#lop-tuyen-sinh").scrollIntoView({block:"start",behavior:"instant"})');
 await check('Class section anchor clears sticky header','document.querySelector("#lop-tuyen-sinh").getBoundingClientRect().top>=document.querySelector(".catalogHeader").getBoundingClientRect().bottom-1');
 await evaluate('scrollTo({top:document.documentElement.scrollHeight,behavior:"instant"})');
 await check('Mobile CTA does not cover footer at page end','document.querySelector(".footer").getBoundingClientRect().bottom<=document.querySelector(".mobileConsultBar").getBoundingClientRect().top+1');
 await shot('u456-mobile-footer-safe-area');
 await viewport(1440,900);await visit('/khoa-hoc/giao-tiep-a1');await evaluate('scrollTo({top:650,behavior:"instant"})');
 await check('Revised enrollment sidebar clears desktop header','document.querySelector(".detailAside").getBoundingClientRect().top>=document.querySelector(".catalogHeader").getBoundingClientRect().bottom+23');
 await shot('u456-detail-sticky-1440');
 await visit('/tu-van?chuong-trinh=tieng-anh&chi-nhanh=chi-nhanh-1&khoa-hoc=giao-tiep-a1&lop=lop-a1');
 await wait('!document.querySelector(".submitButton").disabled');
 await check('Form retains all incoming selections','document.querySelector("select[name=course]").value==="giao-tiep-a1"&&document.querySelector("select[name=schoolClass]").value==="lop-a1"');
 await check('Contact and learning fields have separate accessible groups','Array.from(document.querySelectorAll(".formSection legend")).map(e=>e.textContent).join("|")==="Thông tin liên hệ|Nhu cầu học tập"');
 await evaluate('document.querySelector("input[name=fullName]").value="Nguyễn An";document.querySelector("select[name=branch]").value="chi-nhanh-2";document.querySelector("select[name=branch]").dispatchEvent(new Event("change",{bubbles:true}))');
 await wait('!document.querySelector(".submitButton").disabled');
 await check('Changing branch clears incompatible course/class and keeps contact','document.querySelector("select[name=course]").value===""&&document.querySelector("select[name=schoolClass]").value===""&&document.querySelector("input[name=fullName]").value==="Nguyễn An"');
 await send('Network.enable');await send('Network.setBlockedURLs',{urls:['*api/catalog?*']});
 try{
  await visit('/tu-van?chuong-trinh=tieng-anh&chi-nhanh=chi-nhanh-1');
  await wait('document.querySelector(".inlineButton")&&!document.querySelector(".submitButton").disabled');
  await check('Options failure allows advice at program level','!document.querySelector(".submitButton").disabled&&document.querySelector("select[name=course]").value===""');
  await evaluate('document.querySelector("input[name=fullName]").value="Nguyễn An"');
  await shot('u456-options-error');
 }finally{await send('Network.setBlockedURLs',{urls:[]});}
 await evaluate('document.querySelector(".inlineButton").click()');
 await wait('!document.querySelector(".inlineButton")&&!document.querySelector(".submitButton").disabled');
 await check('Retry restores options without losing contact','document.querySelector("select[name=course]").options.length>1&&document.querySelector("input[name=fullName]").value==="Nguyễn An"');
 return {pages,scope:'Chrome fixture UI; no writes to application MySQL.'};
};
