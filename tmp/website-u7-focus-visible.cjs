const fs=require('node:fs');
const file='D:/hoctap/KLTN/language-center/language-center-website/src/components/consultation-form.tsx';
let source=fs.readFileSync(file,'utf8');
source=source.replace('export function ConsultationForm(',`function revealFocusedControl(form: HTMLFormElement | null) {
  const control = document.activeElement;
  if (!(control instanceof HTMLElement) || !form?.contains(control)) return;
  const viewport = window.visualViewport;
  const top = Math.max(viewport?.offsetTop || 0, document.querySelector(".catalogHeader")?.getBoundingClientRect().bottom || 0) + 16;
  const bottom = (viewport ? viewport.offsetTop + viewport.height : window.innerHeight) - 16;
  const rect = control.getBoundingClientRect();
  if (rect.top < top || rect.bottom > bottom) control.scrollIntoView({ block: "center", behavior: "instant" });
}

export function ConsultationForm(`);
source=source.replace('  const attempt = useRef<', '  const formRoot = useRef<HTMLFormElement>(null);\n  const attempt = useRef<');
source=source.replace('  async function submit(event:',`  useEffect(() => {
    const viewport = window.visualViewport;
    let frame = 0;
    const reveal = () => { cancelAnimationFrame(frame); frame = requestAnimationFrame(() => revealFocusedControl(formRoot.current)); };
    viewport?.addEventListener("resize", reveal);
    return () => { viewport?.removeEventListener("resize", reveal); cancelAnimationFrame(frame); };
  }, []);

  async function submit(event:`);
source=source.replace('<form className="consultationForm" onSubmit=', '<form ref={formRoot} onFocusCapture={() => requestAnimationFrame(() => revealFocusedControl(formRoot.current))} className="consultationForm" onSubmit=');
fs.writeFileSync(file,source);
