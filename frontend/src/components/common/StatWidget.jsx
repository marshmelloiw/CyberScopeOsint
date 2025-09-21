import React from 'react';
import { cn } from '../../lib/utils';

const StatWidget = ({
  title,
  value,
  change,
  changeType = 'neutral', // positive, negative, neutral
  icon: Icon,
  className,
  trend,
  subtitle,
}) => {
  const changeColors = {
    positive: 'text-success',
    negative: 'text-danger',
    neutral: 'text-surface-muted',
  };

  const trendColors = {
    up: 'text-success',
    down: 'text-danger',
    stable: 'text-surface-muted',
  };

  return (
    <div className={cn('glass rounded-2xl p-6', className)}>
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-surface-muted">{title}</p>
          <div className="mt-2 flex items-baseline space-x-2">
            <p className="text-3xl font-bold text-white">{value}</p>
            {change && (
              <span className={cn('text-sm font-medium', changeColors[changeType])}>
                {changeType === 'positive' && '+'}
                {change}
              </span>
            )}
          </div>
          {subtitle && (
            <p className="mt-1 text-sm text-surface-muted">{subtitle}</p>
          )}
        </div>
        {Icon && (
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-500/10">
            <Icon className="h-6 w-6 text-primary-500" />
          </div>
        )}
      </div>
      
      {trend && (
        <div className="mt-4 flex items-center space-x-2">
          <div className={cn('flex items-center text-sm', trendColors[trend.direction])}>
            {trend.direction === 'up' && (
              <svg className="mr-1 h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M12 7a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0V8.414l-4.293 4.293a1 1 0 01-1.414 0L8 10.414l-4.293 4.293a1 1 0 01-1.414-1.414l5-5a1 1 0 011.414 0L12 7z" clipRule="evenodd" />
              </svg>
            )}
            {trend.direction === 'down' && (
              <svg className="mr-1 h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M12 13a1 1 0 100 2h5a1 1 0 001-1v-5a1 1 0 10-2 0v2.586l-4.293-4.293a1 1 0 00-1.414 0L8 9.586l-4.293-4.293a1 1 0 00-1.414 1.414l5 5a1 1 0 001.414 0L12 13z" clipRule="evenodd" />
              </svg>
            )}
            {trend.direction === 'stable' && (
              <svg className="mr-1 h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clipRule="evenodd" />
              </svg>
            )}
            {trend.value}
          </div>
          <span className="text-sm text-surface-muted">vs last period</span>
        </div>
      )}
    </div>
  );
};

export default StatWidget;
