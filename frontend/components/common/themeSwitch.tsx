"use client";

import React, { useCallback } from "react";
import { useTheme } from "next-themes";

import ThemeImage from "./themeImage";

type ThemeSwitchProps = {
  className?: string;
};

const ThemeSwitch = ({ className }: ThemeSwitchProps) => {
  const { theme, setTheme } = useTheme();

  const handleThemeToggle = useCallback(() => {
    if (theme === "light") {
      setTheme("dark");
    } else if (theme === "dark") {
      setTheme("light");
    } else if (theme === "system") {
      const systemTheme = getComputedStyle(
        document.querySelector("html")!,
      ).colorScheme;
      const changeTheme = systemTheme === "light" ? "dark" : "light";
      setTheme(changeTheme);
    }
  }, [theme, setTheme]);

  return (
    <button className={className} onClick={handleThemeToggle}>
      <ThemeImage
        lightSrc="/svgs/moon.svg"
        darkSrc="/svgs/sun.svg"
        alt="다크/밝은 모드"
        width={28}
        height={28}
      />
    </button>
  );
};

export default ThemeSwitch;
