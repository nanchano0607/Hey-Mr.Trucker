// src/utils/isMobile.ts

export function isMobileDevice(): boolean {
  if (typeof navigator === "undefined" || typeof window === "undefined") {
    return false;
  }

  const ua = navigator.userAgent.toLowerCase();
  const isMobileUA =
    /android|iphone|ipad|ipod|opera mini|iemobile|mobile/i.test(ua);

  const isTouch =
    (navigator as any).maxTouchPoints > 1 ||
    window.matchMedia("(pointer: coarse)").matches;

  return isMobileUA || isTouch;
}
