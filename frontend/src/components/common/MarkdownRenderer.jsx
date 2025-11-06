import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { cn } from '../../lib/utils';

const headingClassName = 'text-white font-semibold tracking-tight';

const components = {
  h1: ({ node, ...props }) => (
    <h1 className={cn(headingClassName, 'text-3xl mt-6 mb-4')} {...props} />
  ),
  h2: ({ node, ...props }) => (
    <h2 className={cn(headingClassName, 'text-2xl mt-6 mb-3')} {...props} />
  ),
  h3: ({ node, ...props }) => (
    <h3 className={cn(headingClassName, 'text-xl mt-5 mb-3')} {...props} />
  ),
  h4: ({ node, ...props }) => (
    <h4 className={cn(headingClassName, 'text-lg mt-4 mb-2')} {...props} />
  ),
  h5: ({ node, ...props }) => (
    <h5 className={cn(headingClassName, 'text-base mt-3 mb-2')} {...props} />
  ),
  h6: ({ node, ...props }) => (
    <h6 className={cn(headingClassName, 'text-sm mt-2 mb-2 uppercase tracking-wide')} {...props} />
  ),
  p: ({ node, ...props }) => (
    <p className="text-surface-muted leading-relaxed" {...props} />
  ),
  ul: ({ node, ordered, ...props }) => (
    <ul className="list-disc pl-6 space-y-2" {...props} />
  ),
  ol: ({ node, ordered, ...props }) => (
    <ol className="list-decimal pl-6 space-y-2" {...props} />
  ),
  li: ({ node, ...props }) => <li className="text-surface-muted" {...props} />,
  strong: ({ node, ...props }) => <strong className="text-white" {...props} />,
  em: ({ node, ...props }) => <em className="italic text-surface-muted" {...props} />,
  blockquote: ({ node, ...props }) => (
    <blockquote
      className="border-l-4 border-primary-500/70 pl-4 italic text-surface-muted"
      {...props}
    />
  ),
  code({ inline, className, children, ...props }) {
    if (inline) {
      return (
        <code
          className={cn('px-1 py-0.5 rounded bg-surface-border text-primary-300 text-sm', className)}
          {...props}
        >
          {children}
        </code>
      );
    }

    return (
      <pre className="bg-surface-border/80 rounded-lg p-4 overflow-x-auto text-sm">
        <code className={className} {...props}>
          {children}
        </code>
      </pre>
    );
  },
  table: ({ node, ...props }) => (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse border border-surface-border/60 text-sm" {...props} />
    </div>
  ),
  thead: ({ node, ...props }) => (
    <thead className="bg-surface-panel/70 text-white" {...props} />
  ),
  tbody: ({ node, ...props }) => <tbody className="text-surface-muted" {...props} />,
  tr: ({ node, isHeader, ...props }) => (
    <tr className="border-b border-surface-border/60" {...props} />
  ),
  th: ({ node, ...props }) => (
    <th className="text-left px-3 py-2 font-semibold" {...props} />
  ),
  td: ({ node, ...props }) => (
    <td className="px-3 py-2 align-top" {...props} />
  ),
};

const MarkdownRenderer = ({ content, className }) => {
  if (!content || typeof content !== 'string' || !content.trim()) {
    return null;
  }

  return (
    <div className={cn('prose prose-invert max-w-none', className)}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {content}
      </ReactMarkdown>
    </div>
  );
};

export default MarkdownRenderer;

