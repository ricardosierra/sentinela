---
name: Sentinela
colors:
  surface: '#081425'
  surface-dim: '#081425'
  surface-bright: '#2f3a4c'
  surface-container-lowest: '#040e1f'
  surface-container-low: '#111c2d'
  surface-container: '#152031'
  surface-container-high: '#1f2a3c'
  surface-container-highest: '#2a3548'
  on-surface: '#d8e3fb'
  on-surface-variant: '#c2c6d6'
  inverse-surface: '#d8e3fb'
  inverse-on-surface: '#263143'
  outline: '#8c909f'
  outline-variant: '#424754'
  surface-tint: '#adc6ff'
  primary: '#adc6ff'
  on-primary: '#002e6a'
  primary-container: '#4d8eff'
  on-primary-container: '#00285d'
  inverse-primary: '#005ac2'
  secondary: '#b7c8e1'
  on-secondary: '#213145'
  secondary-container: '#3a4a5f'
  on-secondary-container: '#a9bad3'
  tertiary: '#bec6e0'
  on-tertiary: '#283044'
  tertiary-container: '#8990a8'
  on-tertiary-container: '#22293d'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#004395'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#dae2fd'
  tertiary-fixed-dim: '#bec6e0'
  on-tertiary-fixed: '#131b2e'
  on-tertiary-fixed-variant: '#3f465c'
  background: '#081425'
  on-background: '#d8e3fb'
  surface-variant: '#2a3548'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 57px
    fontWeight: '600'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '500'
    lineHeight: 36px
  title-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-lg:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  margin-mobile: 16px
  margin-tablet: 24px
  gutter: 8px
---

## Brand & Style
The design system for this utility is anchored in the concept of "The Silent Guardian." It prioritizes privacy and security through a highly disciplined **Minimalist / Corporate Modern** aesthetic. The UI should feel like a native extension of the operating system—unobtrusive, dependable, and invisible until needed.

The emotional response should be one of immediate relief and control. By utilizing a Material 3-inspired framework, the design system ensures familiarity while emphasizing its role as a professional security tool. The interface avoids unnecessary decoration, focusing instead on clear status indicators and high-utility workflows.

## Colors
The palette is optimized for **Dark Mode** to reduce eye strain and blend with the native Android environment. 

- **Primary:** A confident "Security Blue" used for active protection states and primary actions.
- **Secondary/Neutral:** A range of Slate Greys (Slate 900 to Slate 500) provide structural hierarchy without creating visual noise.
- **Action Colors:** Green and Red are used strictly for functional status (Allowed vs. Blocked). These are applied with low-chroma backgrounds and high-chroma accents to remain professional rather than alarming.
- **Dynamic Color:** The system is designed to support Android 11+ Monet engine integration, allowing the Primary and Tertiary shades to shift based on the user's wallpaper while maintaining the core contrast ratios for accessibility.

## Typography
**Inter** serves as the primary typeface, chosen for its exceptional legibility at small sizes—critical for call logs and technical data. **Geist** is introduced for labels and monospaced data points (like phone numbers) to lean into the "technical utility" aspect of the brand.

Scale follows the Material 3 type scale. For mobile devices, display and large headlines should scale down to ensure content remains above the fold. All body text maintains a minimum of 14px to ensure accessibility for a wide range of users.

## Layout & Spacing
This design system utilizes the standard **8dp Android Grid**. All spatial relationships are multiples of 8, ensuring a predictable and rhythmic layout.

- **Margins:** Standard mobile views use a 16px side margin. For data-heavy logs, this can be reduced to 12px to maximize content space.
- **Grid:** A 4-column fluid grid for mobile and 8-column for tablet. 
- **Containers:** Content is grouped into logical blocks using padding (16px internal) to separate call history, settings groups, and status indicators.

## Elevation & Depth
In line with Material 3, the system uses **Tonal Layers** rather than heavy shadows. 

- **Surface (Level 0):** The base background of the app (Deepest Blue/Slate).
- **Surface Container (Level 1):** Subtle elevation for cards and list items, achieved by layering the primary color at 5-8% opacity over the base.
- **High Elevation:** Reserved for floating action buttons (FAB) or bottom sheets, using a soft, 12% opacity ambient shadow with a 16px blur to provide a clear "above" relationship.
- **Glassmorphism:** Used sparingly for the Top App Bar when content scrolls beneath it, utilizing a Backdrop Blur (20px) to maintain context while keeping the header readable.

## Shapes
The design system employs a **Rounded** shape language to balance the professional tone with a modern, approachable feel. 

- **Small Components (Buttons/Inputs):** 8px radius.
- **Medium Components (Cards/Dialogs):** 16px radius.
- **Large Components (Bottom Sheets/Onboarding):** 24px radius or fully rounded (pill) for status chips.
This progressive rounding helps users visually distinguish between interactive elements and layout containers.

## Components
- **Buttons:** Primary buttons are high-contrast (Primary Blue with White text). Onboarding buttons use a "Tonal" style—subtle background colors to prevent visual fatigue.
- **Cards:** History items use a flat card with a 1px "low-contrast outline" in Slate 700 to separate entries without the weight of a shadow.
- **Toggle Switches:** Custom Material 3 switches. When "Protection" is ON, the track glows with a subtle primary tint; when OFF, it recedes into the neutral background.
- **Status Chips:** Small, pill-shaped indicators for call types (e.g., "Spam", "Verified", "Blocked"). These use the Action Colors at 15% opacity with high-saturation text.
- **Lists:** High-density lists with 56dp row heights. Every list item includes a leading icon (Avatar or Shield) and a trailing timestamp.
- **Input Fields:** Outlined variants with a focus state that thickens the border to 2px in the Primary Blue color.