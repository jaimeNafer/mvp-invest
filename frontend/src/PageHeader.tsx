type Props = { id: string; title: string; eyebrow?: string; description?: string };

export default function PageHeader({ id, title, eyebrow, description }: Props) {
  return (
    <div className="intro">
      {eyebrow && <p className="eyebrow">{eyebrow}</p>}
      <h1 id={id}>{title}</h1>
      {description && <p className="intro-text">{description}</p>}
    </div>
  );
}
