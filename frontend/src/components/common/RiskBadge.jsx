import React from 'react';
import { cn, riskToColor } from '../../lib/utils';

const RiskBadge = ({ risk, showScore = true, size = 'md', className }) => {
  const riskColor = riskToColor(risk);
  const severity = risk <= 2 ? 'Düşük' : risk <= 4 ? 'Orta' : risk <= 7 ? 'Yüksek' : 'Kritik';
  
  const sizeClasses = {
    sm: 'px-2 py-1 text-xs',
    md: 'px-3 py-1.5 text-sm',
    lg: 'px-4 py-2 text-base',
  };
  
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border font-medium',
        riskColor,
        sizeClasses[size],
        className
      )}
    >
      {showScore && (
        <span className="mr-1 font-bold">{risk}</span>
      )}
      {severity}
    </span>
  );
};

export default RiskBadge;
