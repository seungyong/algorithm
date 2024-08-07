"use client";

import React, { useCallback, useEffect, useRef, useState } from "react";

import ThemeImage from "./themeImage";

import styles from "./dropdown.module.scss";
import { notosansMedium } from "@/styles/_font";

export type DropdownItem = {
  title: string;
  value: string | number;
};

type DropdownProps = {
  isVisible: boolean;
  queryKey?: string;
  defaultTitle: string;
  items: DropdownItem[];
  onVisible: (value: boolean, queryKey?: string) => void;
  onChange: (value: string | number, queryKey?: string) => void;
};

const Dropdown = ({
  isVisible = false,
  queryKey,
  defaultTitle,
  items,
  onChange,
  onVisible,
}: DropdownProps) => {
  const dorpdownRef = useRef<HTMLDivElement>(null);
  const modalRef = useRef<HTMLDivElement>(null);

  const [title, setTitle] = useState<string>(defaultTitle);

  const handleVisible = useCallback(
    (event: MouseEvent) => {
      const dorpdownInside = dorpdownRef.current?.contains(
        event.target as Node,
      );
      const modalInside = modalRef.current?.contains(event.target as Node);

      if (dorpdownInside || modalInside) return;

      onVisible(false, queryKey);
      document.body.removeEventListener("click", handleVisible);
    },
    [queryKey, onVisible],
  );

  const handleClick = useCallback(
    (event: React.MouseEvent<HTMLDivElement>) => {
      event.stopPropagation();
      event.preventDefault();
      onVisible(!isVisible, queryKey || undefined);
    },
    [queryKey, isVisible, onVisible],
  );

  const handleChange = useCallback(
    (event: React.MouseEvent<HTMLLIElement>, item: DropdownItem) => {
      // 상위 요소 클릭 이벤트(handleClick) 전달 제거
      event.stopPropagation();
      setTitle(item.title);
      onChange(item.value, queryKey);
      onVisible(!isVisible, queryKey);
    },
    [isVisible, queryKey, onChange, onVisible],
  );

  useEffect(() => {
    if (isVisible) {
      document.body.addEventListener("click", handleVisible);
    } else {
      document.body.removeEventListener("click", handleVisible);
    }

    return () => {
      document.body.removeEventListener("click", handleVisible);
    };
  }, [isVisible, handleClick, handleVisible]);

  useEffect(() => {
    setTitle(defaultTitle);
  }, [defaultTitle]);

  return (
    <div
      ref={dorpdownRef}
      className={styles.dropdown}
      onClick={(event) => handleClick(event)}
    >
      <button className={styles.button}>
        <span className={notosansMedium.className}>{title}</span>
        <ThemeImage
          lightSrc="/svgs/arrow_down_black.svg"
          darkSrc="/svgs/arrow_down_white.svg"
          alt="화살표 아래"
          width={20}
          height={20}
        />
      </button>
      <div ref={modalRef} className={styles.modal}>
        {isVisible && (
          <ul className={styles.itemBox}>
            {items.map((item, idx: number) => (
              <li
                key={idx}
                onClick={(event) => handleChange(event, item)}
                className={styles.item}
              >
                <button>{item.title}</button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
};

export default Dropdown;
