import type { ComponentProps } from "react";

export default function Input({ className = "", ...props }: ComponentProps<"input">) {
  return <input {...props} className={`ds-control ${className}`.trim()} />;
}
