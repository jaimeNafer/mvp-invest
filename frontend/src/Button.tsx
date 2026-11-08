import type { ComponentProps } from "react";

type Props = ComponentProps<"button"> & { variant?: "primary" | "secondary" | "icon" };

export default function Button({
  variant = "primary",
  type = "button",
  className = "",
  ...props
}: Props) {
  return <button {...props} type={type} className={`${variant}-button ${className}`.trim()} />;
}
