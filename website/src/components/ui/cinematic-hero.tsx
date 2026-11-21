// src/components/ui/cinematic-hero.tsx
"use client";

import React, { useEffect, useRef } from "react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { cn } from "@/lib/utils";

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger);
}

const INJECTED_STYLES = `
  .gsap-reveal { visibility: hidden; }

  /* Environment Overlays */
  .film-grain {
      position: absolute; inset: 0; width: 100%; height: 100%;
      pointer-events: none; z-index: 50; opacity: 0.04; mix-blend-mode: overlay;
      background: url('data:image/svg+xml;utf8,<svg viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg"><filter id="noiseFilter"><feTurbulence type="fractalNoise" baseFrequency="0.8" numOctaves="3" stitchTiles="stitch"/></filter><rect width="100%" height="100%" filter="url(%23noiseFilter)"/></svg>');
  }

  .bg-grid-theme {
      background-size: 48px 48px;
      background-image: 
          linear-gradient(to right, color-mix(in srgb, var(--color-foreground) 4%, transparent) 1px, transparent 1px),
          linear-gradient(to bottom, color-mix(in srgb, var(--color-foreground) 4%, transparent) 1px, transparent 1px);
      mask-image: radial-gradient(ellipse at center, black 0%, transparent 72%);
      -webkit-mask-image: radial-gradient(ellipse at center, black 0%, transparent 72%);
  }

  /* -------------------------------------------------------------------
     MONOCHROME SKEUOMORPHIC MATERIALS (Zero Colorful Slop)
  ---------------------------------------------------------------------- */
  
  /* Outside card: Pure monochrome typography */
  .text-3d-matte {
      color: var(--color-foreground, #F2F2F2);
      text-shadow: 
          0 10px 30px rgba(0, 0, 0, 0.4), 
          0 2px 4px rgba(0, 0, 0, 0.2);
  }

  .text-silver-matte {
      background: linear-gradient(180deg, #FFFFFF 0%, #A6A6A6 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      transform: translateZ(0);
      filter: 
          drop-shadow(0px 8px 16px rgba(0, 0, 0, 0.35)) 
          drop-shadow(0px 1px 2px rgba(0, 0, 0, 0.2));
  }

  /* Inside card: Pure satin titanium / obsidian contrast */
  .text-card-silver-matte {
      background: linear-gradient(180deg, #FFFFFF 0%, #8A8A8E 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      transform: translateZ(0);
      filter: 
          drop-shadow(0px 12px 24px rgba(0,0,0,0.9)) 
          drop-shadow(0px 4px 8px rgba(0,0,0,0.7));
  }

  /* Deep Monochrome Obsidian Card */
  .premium-depth-card {
      background: linear-gradient(145deg, #181818 0%, #0E0E0E 100%);
      box-shadow: 
          0 40px 100px -20px rgba(0, 0, 0, 0.95),
          0 20px 40px -20px rgba(0, 0, 0, 0.85),
          inset 0 1px 2px rgba(255, 255, 255, 0.12),
          inset 0 -2px 4px rgba(0, 0, 0, 0.9);
      border: 1px solid rgba(255, 255, 255, 0.08);
      position: relative;
  }

  .card-sheen {
      position: absolute; inset: 0; border-radius: inherit; pointer-events: none; z-index: 50;
      background: radial-gradient(800px circle at var(--mouse-x, 50%) var(--mouse-y, 50%), rgba(255,255,255,0.06) 0%, transparent 45%);
      mix-blend-mode: screen; transition: opacity 0.3s ease;
  }

  /* Realistic Obsidian Hardware Frame */
  .phone-bezel {
      background-color: #0E0E10;
      box-shadow: 
          inset 0 0 0 2px #383838, 
          inset 0 0 0 6px #000000, 
          0 40px 80px -15px rgba(0,0,0,0.95),
          0 15px 25px -5px rgba(0,0,0,0.75);
      transform-style: preserve-3d;
  }

  .hardware-btn {
      background: linear-gradient(90deg, #3A3A3C 0%, #1A1A1C 100%);
      box-shadow: 
          -2px 0 5px rgba(0,0,0,0.8),
          inset -1px 0 1px rgba(255,255,255,0.12),
          inset 1px 0 2px rgba(0,0,0,0.9);
      border-left: 1px solid rgba(255,255,255,0.05);
  }
  
  .screen-glare {
      background: linear-gradient(115deg, rgba(255,255,255,0.06) 0%, rgba(255,255,255,0) 42%);
  }

  .widget-depth {
      background: linear-gradient(180deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.01) 100%);
      box-shadow: 
          0 10px 20px rgba(0,0,0,0.4),
          inset 0 1px 1px rgba(255,255,255,0.06),
          inset 0 -1px 1px rgba(0,0,0,0.6);
      border: 1px solid rgba(255,255,255,0.05);
  }

  .floating-ui-badge {
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0.02) 100%);
      backdrop-filter: blur(24px); 
      -webkit-backdrop-filter: blur(24px);
      box-shadow: 
          0 0 0 1px rgba(255, 255, 255, 0.08),
          0 25px 50px -12px rgba(0, 0, 0, 0.85),
          inset 0 1px 1px rgba(255,255,255,0.15),
          inset 0 -1px 1px rgba(0,0,0,0.6);
  }

  /* Tactile Monochrome Buttons */
  .btn-monochrome-primary {
      background: linear-gradient(180deg, #FFFFFF 0%, #EDEDED 100%);
      color: #0A0A0A;
      box-shadow: 
          0 0 0 1px rgba(255,255,255,0.2), 
          0 2px 4px rgba(0,0,0,0.2), 
          0 12px 24px -4px rgba(0,0,0,0.4), 
          inset 0 1px 1px #FFFFFF, 
          inset 0 -2px 4px rgba(0,0,0,0.1);
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .btn-monochrome-primary:hover {
      transform: translateY(-2px);
      background: #FFFFFF;
      box-shadow: 
          0 0 0 1px #FFFFFF, 
          0 6px 16px -2px rgba(0,0,0,0.3), 
          0 20px 32px -6px rgba(0,0,0,0.5), 
          inset 0 1px 1px #FFFFFF;
  }
  .btn-monochrome-primary:active {
      transform: translateY(1px);
      background: #E5E5E5;
      box-shadow: 0 0 0 1px rgba(0,0,0,0.2), inset 0 2px 4px rgba(0,0,0,0.15);
  }

  .btn-monochrome-secondary {
      background: linear-gradient(180deg, #222224 0%, #141416 100%);
      color: #F2F2F2;
      border: 1px solid rgba(255,255,255,0.12);
      box-shadow: 
          0 2px 4px rgba(0,0,0,0.5), 
          0 12px 24px -4px rgba(0,0,0,0.7), 
          inset 0 1px 1px rgba(255,255,255,0.12), 
          inset 0 -2px 4px rgba(0,0,0,0.6);
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .btn-monochrome-secondary:hover {
      transform: translateY(-2px);
      background: linear-gradient(180deg, #2C2C30 0%, #1A1A1E 100%);
      border-color: rgba(255,255,255,0.22);
      color: #FFFFFF;
      box-shadow: 0 6px 16px -2px rgba(0,0,0,0.6), 0 20px 32px -6px rgba(0,0,0,0.85);
  }
  .btn-monochrome-secondary:active {
      transform: translateY(1px);
      background: #141416;
      box-shadow: inset 0 2px 6px rgba(0,0,0,0.8);
  }

  .progress-ring {
      transform: rotate(-90deg);
      transform-origin: center;
      stroke-dasharray: 402;
      stroke-dashoffset: 402;
      stroke-linecap: round;
  }
`;

export interface CinematicHeroProps extends React.HTMLAttributes<HTMLDivElement> {
  brandName?: string;
  tagline1?: string;
  tagline2?: string;
  cardHeading?: string;
  cardDescription?: React.ReactNode;
  metricPrefix?: string;
  metricValue?: number;
  metricLabel?: string;
  ctaHeading?: string;
  ctaDescription?: string;
}

export function CinematicHero({ 
  brandName = "KANRI",
  tagline1 = "Your money, on your phone.",
  tagline2 = "Only.",
  cardHeading = "Zero trackers. Zero cloud.",
  cardDescription = (
    <>
      <span className="text-white font-semibold">Kanri</span> reads your bank SMS, auto-categorizes payments, and keeps every transaction sealed on your device with SQLCipher AES-256 encryption.
    </>
  ),
  metricPrefix = "₹",
  metricValue = 1400,
  metricLabel = "Daily Safe-to-Spend",
  ctaHeading = "Take control of your money.",
  ctaDescription = "Fully offline-first personal finance for Android. Zero ads, zero cookies, zero telemetry, and 100% open-source sovereignty.",
  className, 
  ...props 
}: CinematicHeroProps) {
  
  const containerRef = useRef<HTMLDivElement>(null);
  const mainCardRef = useRef<HTMLDivElement>(null);
  const mockupRef = useRef<HTMLDivElement>(null);
  const requestRef = useRef<number>(0);

  // 1. High-Performance Mouse Interaction Logic (Using requestAnimationFrame)
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (window.scrollY > window.innerHeight * 2) return;

      cancelAnimationFrame(requestRef.current);
      
      requestRef.current = requestAnimationFrame(() => {
        if (mainCardRef.current && mockupRef.current) {
          const rect = mainCardRef.current.getBoundingClientRect();
          const mouseX = e.clientX - rect.left;
          const mouseY = e.clientY - rect.top;
          
          mainCardRef.current.style.setProperty("--mouse-x", `${mouseX}px`);
          mainCardRef.current.style.setProperty("--mouse-y", `${mouseY}px`);

          const xVal = (e.clientX / window.innerWidth - 0.5) * 2;
          const yVal = (e.clientY / window.innerHeight - 0.5) * 2;

          gsap.to(mockupRef.current, {
            rotationY: xVal * 10,
            rotationX: -yVal * 10,
            ease: "power3.out",
            duration: 1.2,
          });
        }
      });
    };

    window.addEventListener("mousemove", handleMouseMove);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      cancelAnimationFrame(requestRef.current);
    };
  }, []);

  // 2. Complex Cinematic Scroll Timeline
  useEffect(() => {
    const isMobile = window.innerWidth < 768;

    const ctx = gsap.context(() => {
      gsap.set(".text-track", { autoAlpha: 0, y: 60, scale: 0.85, filter: "blur(20px)", rotationX: -20 });
      gsap.set(".text-days", { autoAlpha: 1, clipPath: "inset(0 100% 0 0)" });
      gsap.set(".main-card", { y: window.innerHeight + 200, autoAlpha: 1 });
      gsap.set([".card-left-text", ".card-right-text", ".mockup-scroll-wrapper", ".floating-badge", ".phone-widget"], { autoAlpha: 0 });
      gsap.set(".cta-wrapper", { autoAlpha: 0, scale: 0.8, filter: "blur(30px)" });

      const introTl = gsap.timeline({ delay: 0.3 });
      introTl
        .to(".text-track", { duration: 1.8, autoAlpha: 1, y: 0, scale: 1, filter: "blur(0px)", rotationX: 0, ease: "expo.out" })
        .to(".text-days", { duration: 1.4, clipPath: "inset(0 0% 0 0)", ease: "power4.inOut" }, "-=1.0");

      const scrollTl = gsap.timeline({
        scrollTrigger: {
          trigger: containerRef.current,
          start: "top top",
          end: "+=7000",
          pin: true,
          scrub: 1,
          anticipatePin: 1,
        },
      });

      scrollTl
        .to([".hero-text-wrapper", ".bg-grid-theme"], { scale: 1.15, filter: "blur(20px)", opacity: 0.2, ease: "power2.inOut", duration: 2 }, 0)
        .to(".main-card", { y: 0, ease: "power3.inOut", duration: 2 }, 0)
        .to(".main-card", { width: "100%", height: "100%", borderRadius: "0px", ease: "power3.inOut", duration: 1.5 })
        .fromTo(".mockup-scroll-wrapper",
          { y: 300, z: -500, rotationX: 50, rotationY: -30, autoAlpha: 0, scale: 0.6 },
          { y: 0, z: 0, rotationX: 0, rotationY: 0, autoAlpha: 1, scale: 1, ease: "expo.out", duration: 2.5 }, "-=0.8"
        )
        .fromTo(".phone-widget", { y: 40, autoAlpha: 0, scale: 0.95 }, { y: 0, autoAlpha: 1, scale: 1, stagger: 0.15, ease: "back.out(1.2)", duration: 1.5 }, "-=1.5")
        .to(".progress-ring", { strokeDashoffset: 80, duration: 2, ease: "power3.inOut" }, "-=1.2")
        .to(".counter-val", { innerHTML: metricValue, snap: { innerHTML: 1 }, duration: 2, ease: "expo.out" }, "-=2.0")
        .fromTo(".floating-badge", { y: 100, autoAlpha: 0, scale: 0.7, rotationZ: -10 }, { y: 0, autoAlpha: 1, scale: 1, rotationZ: 0, ease: "back.out(1.5)", duration: 1.5, stagger: 0.2 }, "-=2.0")
        .fromTo(".card-left-text", { x: -50, autoAlpha: 0 }, { x: 0, autoAlpha: 1, ease: "power4.out", duration: 1.5 }, "-=1.5")
        .fromTo(".card-right-text", { x: 50, autoAlpha: 0, scale: 0.8 }, { x: 0, autoAlpha: 1, scale: 1, ease: "expo.out", duration: 1.5 }, "<")
        .to({}, { duration: 2.5 })
        .set(".hero-text-wrapper", { autoAlpha: 0 })
        .set(".cta-wrapper", { autoAlpha: 1 }) 
        .to({}, { duration: 1.5 })
        .to([".mockup-scroll-wrapper", ".floating-badge", ".card-left-text", ".card-right-text"], {
          scale: 0.9, y: -40, z: -200, autoAlpha: 0, ease: "power3.in", duration: 1.2, stagger: 0.05,
        })
        .to(".main-card", { 
          width: isMobile ? "92vw" : "85vw", 
          height: isMobile ? "92vh" : "85vh", 
          borderRadius: isMobile ? "32px" : "40px", 
          ease: "expo.inOut", 
          duration: 1.8 
        }, "pullback") 
        .to(".cta-wrapper", { scale: 1, filter: "blur(0px)", ease: "expo.inOut", duration: 1.8 }, "pullback")
        .to(".main-card", { y: -window.innerHeight - 300, ease: "power3.in", duration: 1.5 });

    }, containerRef);

    return () => ctx.revert();
  }, [metricValue]); 

  return (
    <div
      ref={containerRef}
      className={cn("relative w-screen h-screen overflow-hidden flex items-center justify-center bg-black text-white font-sans antialiased", className)}
      style={{ perspective: "1500px" }}
      {...props}
    >
      <style dangerouslySetInnerHTML={{ __html: INJECTED_STYLES }} />
      <div className="film-grain" aria-hidden="true" />
      <div className="bg-grid-theme absolute inset-0 z-0 pointer-events-none opacity-40" aria-hidden="true" />

      {/* BACKGROUND LAYER 1: Hero Monochrome Titles */}
      <div className="hero-text-wrapper absolute z-10 flex flex-col items-center justify-center text-center w-screen px-4 will-change-transform transform-style-3d">
        <h1 className="text-track gsap-reveal text-3d-matte text-5xl md:text-7xl lg:text-[5.5rem] font-bold tracking-tight mb-2">
          {tagline1}
        </h1>
        <h1 className="text-days gsap-reveal text-silver-matte text-5xl md:text-7xl lg:text-[5.5rem] font-extrabold tracking-tighter">
          {tagline2}
        </h1>
      </div>

      {/* BACKGROUND LAYER 2: Tactile Monochrome CTAs */}
      <div className="cta-wrapper absolute z-10 flex flex-col items-center justify-center text-center w-screen px-4 gsap-reveal pointer-events-auto will-change-transform">
        <h2 className="text-4xl md:text-6xl lg:text-7xl font-bold mb-4 tracking-tight text-silver-matte">
          {ctaHeading}
        </h2>
        <p className="text-neutral-400 text-base md:text-lg mb-10 max-w-xl mx-auto font-normal leading-relaxed">
          {ctaDescription}
        </p>
        <div className="flex flex-col sm:flex-row gap-5">
          <a
            href="#download"
            className="btn-monochrome-primary flex items-center justify-center gap-3 px-8 py-4 rounded-2xl group focus:outline-none focus:ring-2 focus:ring-white/40"
          >
            <svg className="w-6 h-6 transition-transform group-hover:scale-105" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            <div className="text-left">
              <div className="text-[10px] font-bold tracking-wider text-neutral-600 uppercase leading-tight">Android 8.0+ · 27.1 MB</div>
              <div className="text-lg font-bold leading-none tracking-tight">Download APK</div>
            </div>
          </a>

          <a
            href="https://github.com/omkarnub/Kanri"
            target="_blank"
            rel="noopener noreferrer"
            className="btn-monochrome-secondary flex items-center justify-center gap-3 px-8 py-4 rounded-2xl group focus:outline-none focus:ring-2 focus:ring-white/40"
          >
            <svg className="w-6 h-6 transition-transform group-hover:scale-105" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
            </svg>
            <div className="text-left">
              <div className="text-[10px] font-bold tracking-wider text-neutral-400 uppercase leading-tight">Open Source · GPL-3.0</div>
              <div className="text-lg font-bold leading-none tracking-tight">View on GitHub</div>
            </div>
          </a>
        </div>
      </div>

      {/* FOREGROUND LAYER: The Physical Obsidian Card */}
      <div className="absolute inset-0 z-20 flex items-center justify-center pointer-events-none" style={{ perspective: "1500px" }}>
        <div
          ref={mainCardRef}
          className="main-card premium-depth-card relative overflow-hidden gsap-reveal flex items-center justify-center pointer-events-auto w-[92vw] md:w-[85vw] h-[92vh] md:h-[85vh] rounded-[32px] md:rounded-[40px]"
        >
          <div className="card-sheen" aria-hidden="true" />

          {/* DYNAMIC RESPONSIVE GRID */}
          <div className="relative w-full h-full max-w-7xl mx-auto px-4 lg:px-12 flex flex-col justify-evenly lg:grid lg:grid-cols-3 items-center lg:gap-8 z-10 py-6 lg:py-0">
            
            {/* 1. BRAND EMBLEM & NAME */}
            <div className="card-right-text gsap-reveal order-1 lg:order-3 flex flex-col items-center lg:items-end justify-center z-20 w-full">
              <div className="flex items-center gap-4 mb-2">
                <span className="text-xs uppercase tracking-[0.25em] text-neutral-400 font-bold">OFFLINE LEDGER</span>
                <span className="text-xs px-2 py-0.5 rounded-full border border-neutral-700 bg-neutral-900 text-neutral-300 font-mono">管理</span>
              </div>
              <h2 className="text-5xl md:text-[5.5rem] lg:text-[7rem] font-black uppercase tracking-tighter text-card-silver-matte leading-none">
                {brandName}
              </h2>
            </div>

            {/* 2. CENTER: SMARTPHONE HARDWARE MOCKUP */}
            <div className="mockup-scroll-wrapper order-2 lg:order-2 relative w-full h-[380px] lg:h-[600px] flex items-center justify-center z-10" style={{ perspective: "1000px" }}>
              <div className="relative w-full h-full flex items-center justify-center transform scale-[0.68] md:scale-85 lg:scale-100">
                
                {/* Physical Obsidian Hardware */}
                <div
                  ref={mockupRef}
                  className="relative w-[280px] h-[580px] rounded-[3rem] phone-bezel flex flex-col will-change-transform transform-style-3d"
                >
                  {/* Physical Hardware Buttons */}
                  <div className="absolute top-[120px] -left-[3px] w-[3px] h-[25px] hardware-btn rounded-l-md z-0" aria-hidden="true" />
                  <div className="absolute top-[160px] -left-[3px] w-[3px] h-[45px] hardware-btn rounded-l-md z-0" aria-hidden="true" />
                  <div className="absolute top-[220px] -left-[3px] w-[3px] h-[45px] hardware-btn rounded-l-md z-0" aria-hidden="true" />
                  <div className="absolute top-[170px] -right-[3px] w-[3px] h-[70px] hardware-btn rounded-r-md z-0 scale-x-[-1]" aria-hidden="true" />

                  {/* Inner Screen Container */}
                  <div className="absolute inset-[6px] bg-[#0E0E11] rounded-[2.6rem] overflow-hidden shadow-[inset_0_0_20px_rgba(0,0,0,1)] text-white z-10 flex flex-col">
                    <div className="absolute inset-0 screen-glare z-40 pointer-events-none" aria-hidden="true" />

                    {/* Camera Punch-Hole & Status */}
                    <div className="relative w-full pt-3 px-6 pb-2 flex justify-between items-center text-[10px] text-neutral-400 z-50">
                      <span className="font-mono font-medium">09:41</span>
                      <div className="w-3.5 h-3.5 rounded-full bg-black border border-neutral-800 flex items-center justify-center">
                        <div className="w-1 h-1 rounded-full bg-neutral-600" />
                      </div>
                      <div className="flex items-center gap-1.5 font-mono">
                        <span>5G</span>
                        <div className="w-4 h-2 border border-neutral-500 rounded-sm p-[1px] flex items-center">
                          <div className="h-full w-full bg-neutral-300 rounded-[1px]" />
                        </div>
                      </div>
                    </div>

                    {/* Actual Kanri App Interface */}
                    <div className="relative w-full h-full px-5 pb-6 flex flex-col justify-between">
                      {/* App Header */}
                      <div className="phone-widget flex justify-between items-center pt-2">
                        <div className="flex flex-col">
                          <span className="text-[9px] text-neutral-500 uppercase tracking-widest font-semibold">Ledger</span>
                          <span className="text-lg font-bold tracking-tight text-neutral-100">Safe-to-Spend</span>
                        </div>
                        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-neutral-900 border border-neutral-800 text-[10px] text-neutral-300">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#30A46C]" />
                          <span>Online</span>
                        </div>
                      </div>

                      {/* Center Allowance Dial */}
                      <div className="phone-widget relative w-44 h-44 mx-auto flex items-center justify-center drop-shadow-[0_20px_30px_rgba(0,0,0,0.9)]">
                        <svg className="absolute inset-0 w-full h-full" aria-hidden="true">
                          <circle cx="88" cy="88" r="64" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="10" />
                          <circle className="progress-ring" cx="88" cy="88" r="64" fill="none" stroke="#F2F2F2" strokeWidth="10" />
                        </svg>
                        <div className="text-center z-10 flex flex-col items-center">
                          <span className="text-xs text-neutral-400 font-mono">{metricPrefix}</span>
                          <span className="counter-val text-3xl font-extrabold tracking-tighter text-white font-mono">0</span>
                          <span className="text-[8px] text-neutral-400 uppercase tracking-[0.12em] font-semibold mt-0.5">{metricLabel}</span>
                        </div>
                      </div>

                      {/* Widgets Stack */}
                      <div className="space-y-2.5">
                        {/* Auto-Captured SMS Pill */}
                        <div className="phone-widget widget-depth rounded-xl p-2.5 flex items-center justify-between">
                          <div className="flex items-center gap-2.5">
                            <div className="w-8 h-8 rounded-lg bg-neutral-800/80 border border-white/10 flex items-center justify-center text-xs">
                              💳
                            </div>
                            <div>
                              <div className="text-xs font-semibold text-neutral-200">HDFC Bank · Swiggy</div>
                              <div className="text-[9px] text-neutral-400">Auto-captured via SMS</div>
                            </div>
                          </div>
                          <span className="text-xs font-mono font-bold text-neutral-200">-₹1,950</span>
                        </div>

                        {/* Dual Wallet Status */}
                        <div className="phone-widget widget-depth rounded-xl p-2.5 flex items-center justify-between text-[11px]">
                          <div className="flex items-center gap-1.5 text-neutral-400">
                            <span>Cash</span>
                            <span className="text-neutral-200 font-mono font-medium">₹3,400</span>
                          </div>
                          <div className="h-3 w-[1px] bg-neutral-800" />
                          <div className="flex items-center gap-1.5 text-neutral-400">
                            <span>Online</span>
                            <span className="text-neutral-200 font-mono font-medium">₹24,850</span>
                          </div>
                        </div>
                      </div>

                      {/* Home Bar Indicator */}
                      <div className="w-24 h-1 bg-white/20 rounded-full mx-auto" />
                    </div>
                  </div>
                </div>

                {/* Floating Monochrome Glass Badges */}
                <div className="floating-badge absolute flex top-6 lg:top-12 left-[-15px] lg:left-[-75px] floating-ui-badge rounded-xl lg:rounded-2xl p-3 lg:p-4 items-center gap-3 z-30">
                  <div className="w-8 h-8 lg:w-9 lg:h-9 rounded-xl bg-neutral-800/80 flex items-center justify-center border border-white/10 text-white">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0110 0v4" />
                    </svg>
                  </div>
                  <div>
                    <p className="text-white text-xs lg:text-sm font-bold tracking-tight">SQLCipher AES-256</p>
                    <p className="text-neutral-400 text-[10px] lg:text-xs">Android Keystore Vault</p>
                  </div>
                </div>

                <div className="floating-badge absolute flex bottom-12 lg:bottom-16 right-[-15px] lg:right-[-75px] floating-ui-badge rounded-xl lg:rounded-2xl p-3 lg:p-4 items-center gap-3 z-30">
                  <div className="w-8 h-8 lg:w-9 lg:h-9 rounded-xl bg-neutral-800/80 flex items-center justify-center border border-white/10 text-white">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                  </div>
                  <div>
                    <p className="text-white text-xs lg:text-sm font-bold tracking-tight">Dual-Engine Capture</p>
                    <p className="text-neutral-400 text-[10px] lg:text-xs">0ms On-Device Match</p>
                  </div>
                </div>

              </div>
            </div>

            {/* 3. CARD LEFT TEXT: VALUE PILLAR */}
            <div className="card-left-text gsap-reveal order-3 lg:order-1 flex flex-col justify-center text-center lg:text-left z-20 w-full lg:max-w-none px-4 lg:px-0">
              <h3 className="text-white text-2xl md:text-3xl lg:text-4xl font-bold mb-0 lg:mb-4 tracking-tight">
                {cardHeading}
              </h3>
              <p className="hidden md:block text-neutral-400 text-sm md:text-base lg:text-lg font-normal leading-relaxed mx-auto lg:mx-0 max-w-sm lg:max-w-none">
                {cardDescription}
              </p>
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}
