import {Component, Input} from "@angular/core";
import {LeaderBoardDto, RankDto} from "../../../shiftservice-client";
import { icons } from "../../util/icons";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {DecimalPipe} from "@angular/common";
import {TooltipDirective} from "../../directives/tooltip.directive";

@Component({
  selector: "app-event-leaderboard",
  imports: [
    FaIconComponent,
    DecimalPipe,
    TooltipDirective
  ],
  templateUrl: "./event-leaderboard.component.html",
  styleUrl: "./event-leaderboard.component.scss"
})
export class EventLeaderboardComponent {

  protected ranksTop3: RankDto[] = [];
  protected ranksRest: RankDto[] = [];
  protected ownRank?: RankDto = undefined;
  protected statusMessage = "";

  protected readonly icons = icons;

  @Input()
  public set leaderboard(value: LeaderBoardDto) {

    const ranks = (value.ranks ?? [])
      .sort((a, b) => a.rank - b.rank);

    this.ranksTop3 = ranks.filter(rank => rank.rank <= 3);
    this.ranksRest = ranks.filter(rank => rank.rank > 3);
    this.ownRank = value.ownRank;
    this.statusMessage = this.ownStatusMessage(value);
  }

  private ownStatusMessage(ranks: LeaderBoardDto) {

    if(ranks.ranks.length === 0) {
      return "The leaderboard is empty.\nSign up for a shift to get ranked!";
    }

    const rankMap = new Map<number, RankDto>();
    ranks.ranks.forEach(r => {
      rankMap.set(r.rank, r);
    });

    if(ranks.ownRank === undefined) {
      return "You are not ranked yet. Sign up for a shift to get ranked!";
    } else if(ranks.ownRank.rank === 1) {
      return "Congrats! You have more shift hours than anyone else.";
    } else if(ranks.ownRank.rank < 4) {
      return `You are ranked in the podium!\nYou need ${
        (rankMap.get(ranks.ownRank.rank - 1)?.hours ?? 0) - ranks.ownRank.hours + 1
      } more hours to reach the #1 spot.`;
    } else if(ranks.ownRank.rank <= ranks.size) {
      return `You are ranked in the top 10!\nYou need ${
        (rankMap.get(3)?.hours ?? 0) - ranks.ownRank.hours + 1
      } more hours to reach the podium.`;
    } else {
      return `You are ranked #${ranks.ownRank.rank}.\nYou need ${
        (rankMap.get(10)?.hours ?? 0) - ranks.ownRank.hours + 1
      } more hours to reach the top 10.`;
    }
  }

}
