// Final UI acceptance using real published data from the isolated MySQL copy.
module.exports=async function({evaluate,visit,check,screenshot,send,wait,quick=false}){
 const pages=[],zoom=[],actualZoom=[],browser=process.env.UI_BROWSER==='edge'?'Edge':'Chrome';
 const viewport=(width,height=900,scale=1)=>send('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:scale,mobile:width<700});
 async function ready(){await evaluate('document.fonts.ready.then(()=>true)');await wait('document.documentElement.style.getPropertyValue("--header-height")!==""');}
 async function key(key,code,windowsVirtualKeyCode){for(const type of ['keyDown','keyUp'])await send('Input.dispatchKeyEvent',{type,key,code,windowsVirtualKeyCode,...(type==='keyDown'&&key==='Enter'?{text:'\r',unmodifiedText:'\r'}:{})});}
 await send('DOM.enable');await send('CSS.enable');await viewport(1440);
 await visit('/chuong-trinh');const program=await evaluate('new URL(document.querySelector(".programCard").href).pathname');
 await visit('/chi-nhanh');const branch=await evaluate('new URL(document.querySelector(".branchCard .button").href).pathname');
 await visit('/giao-vien');const teacher=await evaluate('new URL(document.querySelector(".teacherCard .textLink").href).pathname');
 await visit('/khoa-hoc');const courseDetail=await evaluate('new URL(document.querySelector(".courseCard h2 a").href).pathname');
 const routes=['/','/khoa-hoc','/chuong-trinh','/chi-nhanh','/giao-vien',courseDetail,program,branch,teacher,'/tu-van','/chinh-sach-bao-mat','/gioi-thieu'];
 for(const width of quick?[]:[320,360,390,768,1024,1280,1366,1440]){
  await viewport(width,width===1366?768:900);
  for(const route of routes){
   await visit(route);await ready();
   await check(browser+' real '+route+' fits '+width,'document.documentElement.scrollWidth<=innerWidth&&!document.querySelector("main").innerText.includes("Chưa tải được thông tin.")');
   await check(browser+' real '+route+' has one main heading at '+width,'document.querySelectorAll("main h1").length===1');
   const data=await evaluate(`({route:location.pathname,width:innerWidth,header:document.querySelector('.catalogHeader').getBoundingClientRect().height,h1:getComputedStyle(document.querySelector('h1')).fontSize,controls:Array.from(document.querySelectorAll('main input:not([type=checkbox]),main select,main textarea')).filter(e=>{const r=e.getBoundingClientRect();return r.width>10&&r.height>10&&r.right>0}).map(e=>({name:e.name,size:getComputedStyle(e).fontSize,height:e.getBoundingClientRect().height}))})`);
   if(data.controls.length)await check(browser+' controls readable '+route+' at '+width,`Array.from(document.querySelectorAll('main input:not([type=checkbox]),main select,main textarea')).filter(e=>{const r=e.getBoundingClientRect();return r.width>10&&r.height>10&&r.right>0}).every(e=>parseFloat(getComputedStyle(e).fontSize)>=16&&e.getBoundingClientRect().height>=44)`);
   pages.push(data);
   if([360,1366].includes(width)&&['/khoa-hoc',courseDetail,'/tu-van',branch,teacher].includes(route))await screenshot('u7-'+browser.toLowerCase()+'-'+route.slice(1).replaceAll('/','-')+'-'+width);
  }
 }
 // A 1440x900 display at 200% browser zoom has a 720x450 CSS layout viewport.
 // This tests equivalent layout/paint metrics, not an actual browser zoom UI setting.
 await viewport(720,450,2);
 for(const route of quick?[]:['/','/khoa-hoc',courseDetail,'/tu-van']){
  await visit(route);await ready();
  await check(browser+' 200% equivalent layout '+route,'document.documentElement.scrollWidth<=innerWidth&&innerWidth===720&&devicePixelRatio===2');
  await check(browser+' zoom navigation stays reachable '+route,'getComputedStyle(document.querySelector(".menuButton")).display!=="none"&&document.querySelector(".menuButton").getBoundingClientRect().bottom<=innerHeight');
  zoom.push({route,layoutWidth:720,layoutHeight:450,deviceScaleFactor:2,method:'200% equivalent metrics; not physical browser zoom'});
  await screenshot('u7-'+browser.toLowerCase()+'-zoom-equivalent-'+route.slice(1).replaceAll('/','-'));
 }
 await viewport(1440,900);await visit('/');await ready();
 await key('Tab','Tab',9);await check(browser+' first Tab reaches skip link','document.activeElement.classList.contains("skipLink")');
 await key('Enter','Enter',13);await key('Tab','Tab',9);await check(browser+' skip link bypasses navigation','!!document.activeElement.closest("main")');
 await evaluate('document.querySelector(".desktopNav summary").focus()');await key('Enter','Enter',13);await wait('!!document.querySelector(".desktopNav details[open]")');
 await key('Tab','Tab',9);await check(browser+' keyboard enters desktop submenu','!!document.activeElement.closest(".navDropdownPanel")');
 await key('Escape','Escape',27);await check(browser+' Escape returns dropdown focus','!document.querySelector("details[open]")&&document.activeElement.tagName==="SUMMARY"');
 await viewport(360,740);await visit('/');await ready();await evaluate('document.querySelector(".menuButton").focus()');await key('Enter','Enter',13);await wait('!!document.querySelector("#mobile-navigation")');
 await key('Tab','Tab',9);await check(browser+' keyboard enters mobile navigation','!!document.activeElement.closest("#mobile-navigation")');
 await key('Escape','Escape',27);await wait('!document.querySelector("#mobile-navigation")');await check(browser+' mobile Escape restores focus','document.activeElement.classList.contains("menuButton")');
 await visit('/khoa-hoc');await ready();await wait('!document.querySelector(".advancedFilters").open');await evaluate('document.querySelector(".advancedFilters summary").focus()');await key('Enter','Enter',13);await wait('document.querySelector(".advancedFilters").open');await key('Tab','Tab',9);
 await check(browser+' advanced filters work by keyboard','document.activeElement.name==="audience"');
 await visit('/tu-van');await ready();
 await evaluate('document.querySelector("input[name=fullName]").focus()');await key('Tab','Tab',9);await check(browser+' form Tab order follows visible fields','document.activeElement.name==="phone"');
 await evaluate('document.querySelector(".submitButton").click()');await check(browser+' required validation focuses first field','document.activeElement.name==="fullName"&&!document.querySelector(".formSuccess")');
 for(const name of ['fullName','phone','email','branch','program','note']){
  await evaluate(`document.querySelector('[name=${name}]').focus()`);
  await wait(`(()=>{const r=document.activeElement.getBoundingClientRect();return r.top>=document.querySelector('.catalogHeader').getBoundingClientRect().bottom&&r.bottom<=innerHeight;})()`);
  await check(browser+' focused '+name+' clears header',`(()=>{const r=document.activeElement.getBoundingClientRect();return r.top>=document.querySelector('.catalogHeader').getBoundingClientRect().bottom&&r.bottom<=innerHeight;})()`);
 }
 await viewport(360,420);
 await wait('document.activeElement.getBoundingClientRect().top>=document.querySelector(".catalogHeader").getBoundingClientRect().bottom&&document.activeElement.getBoundingClientRect().bottom<=innerHeight');
 await check(browser+' viewport shrink keeps focused field visible','document.activeElement.name==="note"');
 await viewport(360,740);
 await send('Emulation.setEmulatedMedia',{features:[{name:'prefers-reduced-motion',value:'reduce'}]});
 await check(browser+' reduced-motion preference respected','getComputedStyle(document.documentElement).scrollBehavior==="auto"');
 await send('Emulation.setEmulatedMedia',{features:[]});
 await viewport(1440);await visit('/khoa-hoc');await ready();
 await evaluate('document.querySelector(".courseCard h2 a").textContent="Khóa tiếng Anh dành cho người học cần phát triển kỹ năng giao tiếp trong nhiều tình huống học tập và công việc thực tế"');
 await check(browser+' long course title has no overflow','document.documentElement.scrollWidth<=innerWidth');await screenshot('u7-'+browser.toLowerCase()+'-long-course-title');
 const document=await send('DOM.getDocument',{depth:0});const node=await send('DOM.querySelector',{nodeId:document.root.nodeId,selector:'h1'});const fonts=(await send('CSS.getPlatformFontsForNode',{nodeId:node.nodeId})).fonts;
 if(!fonts.length||fonts.some(f=>!f.familyName.startsWith('Be Vietnam Pro')))throw Error(browser+' heading font fallback '+JSON.stringify(fonts));
 await check(browser+' actual Vietnamese heading uses bundled font','document.fonts.status==="loaded"');
 // Change only the fresh test profile's page zoom through its own settings WebUI.
 // No personal browser profile or OS preference is accessed.
 const settings=browser==='Edge'?'edge://settings/appearance':'chrome://settings/appearance';
 async function settingsReady(){await send('Page.navigate',{url:settings});await wait('typeof chrome!=="undefined"&&typeof chrome.settingsPrivate?.getDefaultZoom==="function"');}
 await settingsReady();const previousZoom=await evaluate('chrome.settingsPrivate.getDefaultZoom()');
 try{
  await evaluate('chrome.settingsPrivate.setDefaultZoom(2)');
  await check(browser+' browser setting confirms 200% zoom','chrome.settingsPrivate.getDefaultZoom().then(zoom=>zoom===2)');
  await send('Emulation.clearDeviceMetricsOverride');
  const browserWindow=await send('Browser.getWindowForTarget');await send('Browser.setWindowBounds',{windowId:browserWindow.windowId,bounds:{windowState:'normal',width:1440,height:900}});
  for(const route of ['/','/khoa-hoc',courseDetail,'/tu-van']){
   await visit(route);await ready();
   await check(browser+' actual 200% zoom fits '+route,'document.documentElement.scrollWidth<=innerWidth&&devicePixelRatio>=1.99&&devicePixelRatio<=2.01&&innerWidth>=680&&innerWidth<=740');
   actualZoom.push(await evaluate('({route:location.pathname,width:innerWidth,height:innerHeight,dpr:devicePixelRatio,visualScale:visualViewport.scale})'));
   await screenshot('u7-'+browser.toLowerCase()+'-actual-200-'+route.slice(1).replaceAll('/','-'));
  }
 }finally{await settingsReady();await evaluate('chrome.settingsPrivate.setDefaultZoom('+JSON.stringify(previousZoom)+')');await viewport(1440);await visit('/');}
 return {browser,pages,zoom,actualZoom,fonts,limitations:['No physical phone connected'],source:quick?'Keyboard/zoom on isolated API fixture':'Real services on isolated MySQL copy'};
};
