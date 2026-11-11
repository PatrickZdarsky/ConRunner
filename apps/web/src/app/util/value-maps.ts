import {time} from "../components/inputs/input-time/input-time.component";
import {DatePipe} from "@angular/common";
import {HttpErrorResponse} from "@angular/common/http";

export const mapValue = {
  undefinedIfEmptyString(value: string | null | undefined): string | undefined {
    if(value === null || value === undefined || value.trim() === "") {
      return undefined;
    }
    return value;
  },
  datetimeToUtcDateString(value: Date){
    return `${value.getFullYear()}-${(value.getMonth()+1).toString().padStart(2,"0")}-${value.getDate().toString().padStart(2,"0")}`;
  },
  dateStringAsEndOfDayDatetime(value: string){
    return new Date(
      value
      + "T23:59:59.999Z"
    );
  },
  dateStringAsStartOfDayDatetime(value: string){
    return new Date(
      value
      + "T00:00:00.000Z"
    );
  },
  localDayBeginFromDatetimeString(value: string): Date {
    const date = new Date(value);
    date.setHours(0,0,0,0);
    return date;
  },
  localDayEndFromDatetimeString(value: string): Date{
    const date = new Date(value);
    date.setHours(23,59,59,999);
    return date;
  },
  datetimeStringAsLocalTime(value: string): time{
    const date = new Date(value);
    return {
      hour: date.getHours(),
      minute: date.getMinutes()
    };
  },
  dateAsLocalDateStartOfDayString(value: Date){
    value = new Date(value.setHours(0,0,0,0));
    return value.toISOString();
  },
  dateAsLocalDateEndOfDayString(value: Date){
    value = new Date(value.setHours(23,59,59,999));
    return value.toISOString();
  },
  combineDateAndLocalTime(date: Date | undefined, localTime: time | undefined){
    if(date === undefined) {
      return undefined;
    }

    const combined = new Date(date.getTime());
    if(localTime !== undefined) {
      combined.setHours(localTime.hour, localTime.minute, 0, 0);
    } else {
      combined.setHours(0, 0, 0, 0);
    }
    return combined;
  },
  /**
   * Concat start and end date as intuitive string in user timezone
   * If dates are the same: day year, hour:minute to hour:minute
   * If dates are different: day year hour:minute to day year hour:minute
   * If dates and hours are the same: day year hour:minute-minute
   * @param start
   * @param end
   * @param datePipe
   */
  dateRangeToDateTimeString(start: Date, end: Date, datePipe: DatePipe): string {
    const sameDay = start.getFullYear() === end.getFullYear()
      && start.getMonth() === end.getMonth()
      && start.getDate() === end.getDate();
    const sameHour = sameDay
      && start.getHours() === end.getHours();

    if(sameHour) {
      return `${
        datePipe.transform(start, "mediumDate")
      } ${
        start.getHours().toString().padStart(2,"0")
      }:${
        start.getMinutes().toString().padStart(2,"0")
      }-${
        end.getMinutes().toString().padStart(2,"0")
      }`;
    } else if(sameDay) {
      return `${
        datePipe.transform(start, "mediumDate")
      } ${
        start.getHours().toString().padStart(2,"0")
      }:${
        start.getMinutes().toString().padStart(2,"0")
      } to ${
        end.getHours().toString().padStart(2,"0")
      }:${
        end.getMinutes().toString().padStart(2,"0")
      }`;
    } else {
      return `${
        datePipe.transform(start, "mediumDate")
      } ${
        start.getHours().toString().padStart(2,"0")
      }:${
        start.getMinutes().toString().padStart(2,"0")
      } to ${
        datePipe.transform(end, "mediumDate")
      } ${
        end.getHours().toString().padStart(2,"0")
      }:${
        end.getMinutes().toString().padStart(2,"0")
      }`;
    }
  },
  /**
   * Concat start and end date as intuitive string in user timezone
   * Don't repeat year, and month parts if same
   * @param start
   * @param end
   * @param datePipe
   */
  dateRangeToDateString(start: Date, end: Date, datePipe: DatePipe): string {
    const sameYear = start.getFullYear() === end.getFullYear();
    const sameMonth = sameYear && start.getMonth() === end.getMonth();
    const sameDay = sameMonth && start.getDate() === end.getDate();

    if(sameDay) {
      return `${
        datePipe.transform(start, "longDate")
      }`;
    } else if(sameMonth) {
      return `${
        datePipe.transform(start, "MMMM d")
      } to ${
        datePipe.transform(end, "d, y")
      }`;
    } else if(sameYear) {
      return `${
        datePipe.transform(start, "MMMM d")
      } to ${
        datePipe.transform(end, "MMMM d, y")
      }`;
    } else {
      return `${
        datePipe.transform(start, "longDate")
      } to ${
        datePipe.transform(end, "longDate")
      }`;
    }
  },

  apiErrorToMessage(error: unknown) {
    if(error instanceof HttpErrorResponse) {
      const httpError = error as HttpErrorResponse;
      if(typeof(httpError.error) === "object") {
        const apiError = httpError.error as object;
        const message = apiError["message" as keyof typeof apiError] as unknown;
        if(typeof(message) === "string") {
          return message;
        }
      }
    }
    return "An unknown error occurred";
  }
};
